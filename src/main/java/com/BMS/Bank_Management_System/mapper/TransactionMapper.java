package com.BMS.Banking_Management_System.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper (componentModel = "spring")
public interface TransactionMapper {
    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "performedByUsername", expression = "java(tx.getPerformedBy()!=null?tx.getPerformedBy().getUsername():null)")
    TransactionDTO toDto(Transaction tx);

}
