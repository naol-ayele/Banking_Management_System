package com.BMS.Bank_Management_System.repository;

import com.BMS.Bank_Management_System.entity.ChatRoom;
import com.BMS.Bank_Management_System.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findByTypeAndActiveTrue(ChatRoom.ChatRoomType type);

    @Query("SELECT cr FROM ChatRoom cr " +
            "JOIN cr.participants cp " +
            "WHERE cp.user.id = :userId AND cr.active = true " +
            "ORDER BY cr.updatedAt DESC")
    List<ChatRoom> findChatRoomsByUserId(@Param("userId") Long userId);

    @Query("SELECT cr FROM ChatRoom cr " +
            "JOIN cr.participants cp " +
            "WHERE cp.user.id = :userId AND cr.type = :type AND cr.active = true")
    List<ChatRoom> findChatRoomsByUserIdAndType(@Param("userId") Long userId, @Param("type") ChatRoom.ChatRoomType type);

    @Query("SELECT cr FROM ChatRoom cr " +
            "WHERE cr.type = 'PRIVATE' AND cr.active = true " +
            "AND EXISTS (SELECT cp1 FROM ChatParticipant cp1 WHERE cp1.chatRoom = cr AND cp1.user.id = :userId1) " +
            "AND EXISTS (SELECT cp2 FROM ChatParticipant cp2 WHERE cp2.chatRoom = cr AND cp2.user.id = :userId2)")
    Optional<ChatRoom> findPrivateChatRoomBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Query("SELECT COUNT(cr) FROM ChatRoom cr " +
            "JOIN cr.participants cp " +
            "WHERE cp.user.id = :userId AND cr.active = true")
    long countActiveChatRoomsByUserId(@Param("userId") Long userId);
}
