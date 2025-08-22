package com.BMS.Bank_Management_System.repository;

import com.BMS.Bank_Management_System.entity.ChatParticipant;
import com.BMS.Bank_Management_System.entity.ChatRoom;
import com.BMS.Bank_Management_System.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    List<ChatParticipant> findByChatRoomAndActiveTrue(ChatRoom chatRoom);

    List<ChatParticipant> findByUserAndActiveTrue(User user);

    Optional<ChatParticipant> findByUserAndChatRoom(User user, ChatRoom chatRoom);

    @Query("SELECT cp FROM ChatParticipant cp " +
            "WHERE cp.chatRoom.id = :chatRoomId AND cp.active = true")
    List<ChatParticipant> findByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT cp FROM ChatParticipant cp " +
            "WHERE cp.user.id = :userId AND cp.active = true")
    List<ChatParticipant> findByUserId(@Param("userId") Long userId);

    @Query("SELECT cp FROM ChatParticipant cp " +
            "WHERE cp.chatRoom.id = :chatRoomId AND cp.user.id = :userId AND cp.active = true")
    Optional<ChatParticipant> findByChatRoomIdAndUserId(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    @Query("UPDATE ChatParticipant cp SET cp.lastReadAt = :lastReadAt " +
            "WHERE cp.chatRoom.id = :chatRoomId AND cp.user.id = :userId")
    void updateLastReadAt(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId, @Param("lastReadAt") LocalDateTime lastReadAt);

    @Query("SELECT COUNT(cp) FROM ChatParticipant cp " +
            "WHERE cp.chatRoom.id = :chatRoomId AND cp.active = true")
    long countActiveParticipantsByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}
