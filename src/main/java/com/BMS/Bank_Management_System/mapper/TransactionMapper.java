package com.BMS.Bank_Management_System.mapper;


import com.BMS.Bank_Management_System.dto.TransactionDTO;
import com.BMS.Bank_Management_System.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper (componentModel = "spring")
public interface TransactionMapper {
    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "performedByUsername", expression = "java(tx.getPerformedBy()!=null?tx.getPerformedBy().getUsername():null)")
    TransactionDTO toDto(Transaction tx);

}
