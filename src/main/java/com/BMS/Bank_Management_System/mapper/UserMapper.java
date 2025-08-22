package com.BMS.Bank_Management_System.mapper;


import com.BMS.Bank_Management_System.dto.UserDTO;
import com.BMS.Bank_Management_System.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "role", expression = "java(user.getRole()!=null?user.getRole().name():null)")
    UserDTO toDto(User user);
}



