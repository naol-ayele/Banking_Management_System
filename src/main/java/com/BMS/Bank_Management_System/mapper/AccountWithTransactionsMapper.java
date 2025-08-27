package com.BMS.Bank_Management_System.mapper;

import com.BMS.Bank_Management_System.dto.AccountWithTransactionsDTO;
import com.BMS.Bank_Management_System.dto.TransactionSimpleDTO;
import com.BMS.Bank_Management_System.entity.Account;
import com.BMS.Bank_Management_System.entity.Transaction;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {TransactionMapper.class})
public interface AccountWithTransactionsMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "customerUsername", expression = "java(account.getUser() != null ? account.getUser().getUsername() : \"Unknown\")")
    @Mapping(target = "customerEmail", expression = "java(account.getUser() != null ? account.getUser().getEmail() : \"\")")
    @Mapping(target = "accountType", expression = "java(account.getAccountType() != null ? account.getAccountType().name() : null)")
    @Mapping(target = "status", expression = "java(account.getStatus() != null ? account.getStatus().name() : null)")
    @Mapping(target = "sentTransactions", ignore = true)
    AccountWithTransactionsDTO toDto(Account account);

    @AfterMapping
    default void mapTransactions(@MappingTarget AccountWithTransactionsDTO dto, Account account) {
        if (account.getSentTransactions() != null) {
            List<TransactionSimpleDTO> simpleTransactions = account.getSentTransactions().stream()
                    .map(this::toTransactionSimpleDTO)
                    .collect(Collectors.toList());
            dto.setSentTransactions(simpleTransactions);
        }
    }

    @Mapping(target = "performedBy", expression = "java(tx.getPerformedBy() != null ? tx.getPerformedBy().getUsername() : \"SYSTEM\")")
    TransactionSimpleDTO toTransactionSimpleDTO(Transaction tx);
}

