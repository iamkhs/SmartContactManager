package com.iamkhs.contactmanager.service.impl;

import com.iamkhs.contactmanager.entities.Contact;
import com.iamkhs.contactmanager.repository.ContactsRepository;
import com.iamkhs.contactmanager.service.ContactService;
import org.springframework.stereotype.Service;

@Service
public class ContactServiceImpl implements ContactService {

    private final ContactsRepository contactsRepository;

    public ContactServiceImpl(ContactsRepository contactsRepository) {
        this.contactsRepository = contactsRepository;
    }

    @Override
    public Contact getContactById(Long id) {
        return this.contactsRepository.findById(id).orElseThrow();
    }
}
