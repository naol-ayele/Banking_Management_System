package com.BMS.Bank_Management_System.mapper;

import com.BMS.Bank_Management_System.dto.AccountDTO;
import com.BMS.Bank_Management_System.entity.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "accountType", expression = "java(acc.getAccountType()!=null?acc.getAccountType().name():null)")
    @Mapping(target = "status", expression = "java(acc.getStatus()!=null?acc.getStatus().name():null)")
    AccountDTO toDto(Account acc);
}

