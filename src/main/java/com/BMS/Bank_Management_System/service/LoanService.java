package com.BMS.Bank_Management_System.service;

import com.BMS.Bank_Management_System.config.LoanProperties;
import com.BMS.Bank_Management_System.dto.loan.LoanApplicationRequest;
import com.BMS.Bank_Management_System.dto.loan.LoanResponse;
import com.BMS.Bank_Management_System.dto.loan.RepaymentRequest;
import com.BMS.Bank_Management_System.entity.*;
import com.BMS.Bank_Management_System.exception.InsufficientBalanceException;
import com.BMS.Bank_Management_System.exception.ResourceNotFoundException;
import com.BMS.Bank_Management_System.mapper.LoanMapper;
import com.BMS.Bank_Management_System.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository repaymentRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final LoanMapper loanMapper;
    private final LoanProperties props;

    // ---------- CUSTOMER ACTIONS ----------

    public LoanResponse apply(String username, LoanApplicationRequest req) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = accountRepository.findById(req.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!account.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Account does not belong to current user");
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not ACTIVE");
        }

        boolean hasOutstanding = loanRepository.existsByUserAndStatusIn(
                user,
                List.of(LoanStatus.PENDING, LoanStatus.APPROVED)
        );
        if (hasOutstanding) {
            throw new IllegalStateException("You already have an outstanding loan. Repay it before applying again.");
        }

        // Eligibility: prior transactions count within lookback window
        var lookbackStart = LocalDateTime.now().minusMonths(props.getLookbackMonths());
        List<Transaction> txns = transactionRepository.findByAccount_Id(account.getId()).stream()
                .filter(t -> t.getTimestamp() != null && !t.getTimestamp().isBefore(lookbackStart))
                .toList();
        if (txns.size() < props.getMinTransactions()) {
            throw new IllegalArgumentException("Not eligible: requires at least " +
                    props.getMinTransactions() + " transactions in last " + props.getLookbackMonths() + " months");
        }

        // ---- NEW: Additional eligibility check based on inflow brackets ----
        BigDecimal inflow = txns.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Determine max eligible loan
        BigDecimal maxEligible;
        if (inflow.compareTo(props.getLowBracketMax()) <= 0) {
            maxEligible = props.getLowMaxLoan();
        } else if (inflow.compareTo(props.getMediumBracketMax()) <= 0) {
            maxEligible = props.getMediumMaxLoan();
        } else {
            maxEligible = props.getHighMaxLoan();
        }

        if (req.getPrincipal().compareTo(maxEligible) > 0) {
            throw new IllegalArgumentException("Requested loan exceeds your eligibility limit. Max allowed: " + maxEligible);
        }
        // -------------------------------------------------------------------

        BigDecimal interestRate = (req.getInterestRate() != null) ? req.getInterestRate() : props.getBaseInterestRate();
        LocalDate start = LocalDate.now();
        LocalDate due = start.plusDays(req.getTermDays());

        BigDecimal baseInterest = req.getPrincipal().multiply(interestRate); // one-shot term interest
        BigDecimal totalDue = req.getPrincipal().add(baseInterest);

        Loan loan = Loan.builder()
                .user(user)
                .account(account)
                .principal(req.getPrincipal())
                .interestRate(interestRate)
                .termDays(req.getTermDays())
                .startDate(start)
                .dueDate(due)
                .status(LoanStatus.PENDING)
                .totalDue(totalDue)
                .outstanding(totalDue)
                .penaltyPercentPerDay(props.getPenaltyPercentPerDay())
                .remark(req.getReason())
                .build();

        loan = loanRepository.save(loan);

        notificationService.notifyUser(user.getId(), "LOAN_SUBMITTED",
                "Your loan request has been submitted and is pending approval.");
        return loanMapper.toDto(loan);
    }

    public LoanResponse repay(Long loanId, RepaymentRequest req, String username) {
        Loan loan = loadLoan(loanId);
        if (loan.getStatus() == LoanStatus.REJECTED) {
            throw new IllegalStateException("Rejected loan cannot be repaid");
        }
        if (loan.getStatus() == LoanStatus.PENDING) {
            throw new IllegalStateException("Pending loan not yet disbursed");
        }
        if (loan.getStatus() == LoanStatus.COMPLETED) {
            throw new IllegalStateException("Loan is already completed");
        }

        // Apply penalty up to now (idempotent per call)
        applyOverduePenaltyIfAny(loan);

        Long fromId = (req.getFromAccountId() != null) ? req.getFromAccountId() : loan.getAccount().getId();
        Account payer = accountRepository.findById(fromId)
                .orElseThrow(() -> new ResourceNotFoundException("Payer account not found"));

        if (!payer.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("Not allowed to pay from this account");
        }
        if (payer.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Payer account is not ACTIVE");
        }

        if (payer.getBalance().compareTo(req.getAmount()) < 0) {
            throw new InsufficientBalanceException("Not enough funds to repay");
        }

        payer.setBalance(payer.getBalance().subtract(req.getAmount()));
        accountRepository.save(payer);

        BigDecimal before = loan.getOutstanding();
        BigDecimal after = before.subtract(req.getAmount());
        if (after.compareTo(BigDecimal.ZERO) <= 0) {
            loan.setOutstanding(BigDecimal.ZERO);
            loan.setStatus(LoanStatus.COMPLETED);
        } else {
            loan.setOutstanding(after);
        }
        loanRepository.save(loan);

        String ref = "LR-" + System.nanoTime();
        transactionRepository.save(Transaction.builder()
                .account(payer)
                .fromAccount(payer)
                .toAccount(null)
                .amount(req.getAmount())
                .type("LOAN_REPAYMENT")
                .timestamp(LocalDateTime.now())
                .status("SUCCESS")
                .referenceId(ref)
                .description("Loan repayment for loan #" + loan.getId())
                .performedBy(payer.getUser())
                .build());

        RepaymentStatus rs;
        if (loan.getStatus() == LoanStatus.COMPLETED) {
            rs = (LocalDate.now().isAfter(loan.getDueDate())) ? RepaymentStatus.LATE : RepaymentStatus.COMPLETED;
        } else {
            rs = (LocalDate.now().isAfter(loan.getDueDate())) ? RepaymentStatus.LATE : RepaymentStatus.PARTIAL;
        }

        repaymentRepository.save(LoanRepayment.builder()
                .loan(loan)
                .account(payer)
                .amount(req.getAmount())
                .paidAt(LocalDateTime.now())
                .status(rs)
                .referenceId(ref)
                .note(req.getNote())
                .build());

        notificationService.notifyUser(loan.getUser().getId(), "LOAN_REPAYMENT",
                "Repayment of " + req.getAmount() + " received for loan #" + loan.getId() + ".");
        return loanMapper.toDto(loan);
    }

    public List<LoanResponse> myLoans(String username) {
        return loanRepository.findByUser_Username(username).stream().map(loanMapper::toDto).toList();
    }


    public List<LoanResponse> listPending() {
        return loanRepository.findByStatus(LoanStatus.PENDING).stream().map(loanMapper::toDto).toList();
    }

    public LoanResponse approve(Long loanId, String officerUsername, String remark) {
        Loan loan = loadLoan(loanId);
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new IllegalStateException("Only PENDING loans can be approved");
        }

        Account acct = loan.getAccount();
        acct.setBalance(acct.getBalance().add(loan.getPrincipal()));
        accountRepository.save(acct);

        String ref = "LN-" + System.nanoTime();
        transactionRepository.save(Transaction.builder()
                .account(acct)
                .fromAccount(null)
                .toAccount(acct)
                .amount(loan.getPrincipal())
                .type("LOAN_DISBURSE")
                .timestamp(LocalDateTime.now())
                .status("SUCCESS")
                .referenceId(ref)
                .description("Loan disbursed #" + loan.getId())
                .performedBy(acct.getUser())
                .build());

        loan.setStatus(LoanStatus.APPROVED);
        loan.setRemark(remark);
        loanRepository.save(loan);

        notificationService.notifyUser(loan.getUser().getId(), "LOAN_APPROVED",
                "Your loan #" + loan.getId() + " has been approved and disbursed.");
        return loanMapper.toDto(loan);
    }

    public LoanResponse reject(Long loanId, String officerUsername, String remark) {
        Loan loan = loadLoan(loanId);
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new IllegalStateException("Only PENDING loans can be rejected");
        }
        loan.setStatus(LoanStatus.REJECTED);
        loan.setRemark(remark);
        loanRepository.save(loan);
        notificationService.notifyUser(loan.getUser().getId(), "LOAN_REJECTED",
                "Your loan #" + loan.getId() + " has been rejected.");
        return loanMapper.toDto(loan);
    }

    public LoanResponse getOne(Long id) {
        return loanMapper.toDto(loadLoan(id));
    }

    public List<LoanResponse> listAll() {
        return loanRepository.findAll().stream().map(loanMapper::toDto).toList();
    }


    private Loan loadLoan(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
    }

    private void applyOverduePenaltyIfAny(Loan loan) {
        LocalDate today = LocalDate.now();
        if (today.isAfter(loan.getDueDate())) {
            long daysOverdue = loan.getDueDate().until(today).getDays();
            if (daysOverdue > 0) {
                BigDecimal penalty = loan.getPrincipal()
                        .multiply(loan.getPenaltyPercentPerDay())
                        .multiply(new BigDecimal(daysOverdue));
                loan.setOutstanding(loan.getOutstanding().add(penalty));
                loanRepository.save(loan);
            }
        }
    }

    public String currentUsername() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null ? a.getName() : null;
    }

    @Transactional
    public void recordRepayment(Long repaymentId, BigDecimal payAmount, Long paidByUserId) {
        LoanRepayment r = repaymentRepository.findById(repaymentId)
                .orElseThrow(() -> new IllegalArgumentException("Repayment not found"));

        BigDecimal already = r.getAmountPaid() == null ? BigDecimal.ZERO : r.getAmountPaid();
        r.setAmountPaid(already.add(payAmount));
        r.setPaidAt(LocalDateTime.now());

        BigDecimal due = r.getAmountDue();
        if (r.getAmountPaid().compareTo(due) >= 0) {
            r.setStatus(RepaymentStatus.COMPLETED);
        } else {
            r.setStatus(RepaymentStatus.PARTIAL);
        }
        repaymentRepository.save(r);

        Loan loan = r.getLoan();
        if (loan.getOutstanding() != null) {
            loan.setOutstanding(loan.getOutstanding().subtract(payAmount));
            if (loan.getOutstanding().signum() <= 0) {
                loan.setOutstanding(BigDecimal.ZERO);
                loan.setStatus(LoanStatus.COMPLETED);
            }
            loanRepository.save(loan);
        }

        notificationService.notifyUser(
                loan.getUser().getId(),
                "PAYMENT_RECEIVED",
                "We received your payment of " + payAmount + " for loan #" + loan.getId() + ". Thank you.");
    }
}
