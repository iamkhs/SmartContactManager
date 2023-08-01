package com.iamkhs.contactmanager.repository;

import com.iamkhs.contactmanager.entities.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactsRepository extends JpaRepository<Contact, Long> {
    // pagination..

    @Query("from Contact as c where c.user.id =:userId")
    //pageable = current-page, contact per-page = 5
    Page<Contact> findContactByUser(@Param("userId") Long userId, Pageable pageable);

}
