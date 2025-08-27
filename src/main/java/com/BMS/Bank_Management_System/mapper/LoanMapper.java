package com.BMS.Bank_Management_System.mapper;

import com.BMS.Bank_Management_System.dto.loan.LoanResponse;
import com.BMS.Bank_Management_System.entity.Loan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LoanMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "status", expression = "java(loan.getStatus().name())")
    LoanResponse toDto(Loan loan);
}
