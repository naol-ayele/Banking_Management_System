package com.BMS.Bank_Management_System.repository;

import  com.BMS.Bank_Management_System.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m " +
            "WHERE m.chatRoom.id = :chatRoomId AND m.deleted = false " +
            "ORDER BY m.sentAt DESC")
    Page<Message> findByChatRoomIdOrderBySentAtDesc(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    @Query("SELECT m FROM Message m " +
            "WHERE m.chatRoom.id = :chatRoomId AND m.sentAt > :since AND m.deleted = false " +
            "ORDER BY m.sentAt ASC")
    List<Message> findNewMessagesSince(@Param("chatRoomId") Long chatRoomId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(m) FROM Message m " +
            "WHERE m.chatRoom.id = :chatRoomId AND m.sentAt > :since AND m.deleted = false")
    long countUnreadMessages(@Param("chatRoomId") Long chatRoomId, @Param("since") LocalDateTime since);

    @Query("SELECT m FROM Message m " +
            "WHERE m.chatRoom.id = :chatRoomId AND m.sender.id = :senderId AND m.deleted = false " +
            "ORDER BY m.sentAt DESC")
    List<Message> findMessagesBySenderInChatRoom(@Param("chatRoomId") Long chatRoomId, @Param("senderId") Long senderId);

    @Query("SELECT m FROM Message m " +
            "WHERE m.chatRoom.id = :chatRoomId AND m.content LIKE %:searchTerm% AND m.deleted = false " +
            "ORDER BY m.sentAt DESC")
    Page<Message> searchMessagesInChatRoom(@Param("chatRoomId") Long chatRoomId, @Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT m FROM Message m " +
            "WHERE m.sender.id = :userId AND m.deleted = false " +
            "ORDER BY m.sentAt DESC")
    Page<Message> findMessagesByUser(@Param("userId") Long userId, Pageable pageable);
}
