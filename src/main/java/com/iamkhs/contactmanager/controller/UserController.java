package com.iamkhs.contactmanager.controller;

import com.iamkhs.contactmanager.entities.Contact;
import com.iamkhs.contactmanager.entities.User;
import com.iamkhs.contactmanager.helper.Messages;
import com.iamkhs.contactmanager.repository.ContactsRepository;
import com.iamkhs.contactmanager.service.UserService;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ContactsRepository contactsRepository;
    private final JavaMailSender mailSender;

    // method for adding common data to response
    @ModelAttribute
    private void addCommonData(Model model, Principal principal){
        if (principal != null) {
            String email = principal.getName();
            // get the user using username(email)
            User user = this.userService.getUser(email);

            model.addAttribute("user", user);
        }
    }

    public UserController(UserService userService, BCryptPasswordEncoder passwordEncoder, ContactsRepository contactsRepository, JavaMailSender mailSender) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.contactsRepository = contactsRepository;
        this.mailSender = mailSender;
    }

    // home dashboard
    @GetMapping("/dashboard")
    public String dashboard(Model model){
        model.addAttribute("title", "User Dashboard");
        return "normal/user-dashboard";
    }


    // open add Contact form handler
    @GetMapping("/add-contact")
    public String openAddContactForm(Model model){
        model.addAttribute("title", "Add Contact");
        model.addAttribute("contact", new Contact());
        return "normal/add-contact";
    }


    // form processing for creating User
    @PostMapping("/form-process")
    public String processForm(@ModelAttribute @Valid User user,
                              BindingResult bindingResult,
                              @RequestParam(value = "agreement", defaultValue = "false") boolean agreement,
                              Model model, HttpSession session,
                              HttpServletRequest request){

        try{
            if (bindingResult.hasErrors()){
                System.out.println(bindingResult);
                model.addAttribute("user", user);
                return "signup";
            }

            if (!agreement){
                throw new Exception("You have to agree the terms and condition");
            }

            user.setRole("ROLE_USER");

            if (!user.getPassword().equals(user.getConfirmPassword())){
                throw new Exception("password & confirm password not match!!");
            }

            // encoding the password
            String encodedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);

            String encodedConfirmPassword = passwordEncoder.encode(user.getConfirmPassword());
            user.setConfirmPassword(encodedConfirmPassword);

            System.err.println(user);

            // Generating the verification code and setting to the user
            String verificationCode = UUID.randomUUID().toString();
            System.err.println("verificationCode before hash : " + verificationCode);

            String hashedVerificationCode = hashUUID(verificationCode);
            System.err.println("verificationCode after hash : " + hashedVerificationCode);
            user.setVerificationCode(hashedVerificationCode);

            user.setUserRegisterDate(LocalDateTime.now());

            // Saving User to the database
            User result = this.userService.saveUser(user);

            // sending the verification code
            sendVerificationEmail(user, request);

            model.addAttribute("user", result);

            model.addAttribute("user", new User());
            session.setAttribute("message", new Messages("Successfully Registered", "alert-success"));

            model.addAttribute("logged", "success");
            return "register-success";

        }catch (Exception e){
            model.addAttribute("user", user);
            session.setAttribute("message", new Messages("Something went wrong!!"+ e.getMessage(), "alert-danger"));
            return "signup";
        }
    }

    // Hashing the UUID for more security
    public String hashUUID(String uuid){
        return DigestUtils.sha256Hex(uuid);
    }

    // Sending Verification code process
    public void sendVerificationEmail(User user, HttpServletRequest request) {
        try {
            String subject = "Please Verify your registration";
            String mailContent = messageContent(user, request);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(mailContent, true); // Set the second parameter to true to indicate HTML content

            mailSender.send(message);

            System.err.println("Verification Email Sent.");

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static String messageContent(User user, HttpServletRequest request) {
        String mailContent = "<p style=\"font-size: 16px;\">Dear " + user.getName() + ",</p>";
        mailContent += "<p style=\"font-size: 16px;\">Please click the link below to verify your registration:</p>";

        String verifyUrl = request.getRequestURL().toString();
        verifyUrl += "/verify?code=" + user.getVerificationCode();

        mailContent += "<h3 style=\"font-size: 18px;\"><a href=\"" + verifyUrl + "\">VERIFY</a></h3>";
        mailContent += "<p style=\"font-size: 14px;\"><strong>This verification link will expire in one minute. " +
                "If you don't verify your registration within one minute, " +
                "you will need to register again.</strong></p>";
        mailContent += "<p style=\"font-size: 16px;\"><strong>Thank You.</strong><br>The <strong>NotesVenture Team</strong></p>";
        return mailContent;
    }


    // Verifying
    @GetMapping("/form-process/verify")
    public String verifyAccount(@RequestParam("code") String code, Model model, HttpSession session){
        System.err.println("param : "+code);
        if (userService.isVerified(code)){
            session.setAttribute("message", new Messages("Account Verified Successfully.", "alert-success"));
        }
        else{
            session.setAttribute("message", new Messages("Something went wrong or Invalid verification code!", "alert-danger"));
        }
        return "redirect:/login";
    }


    // Processing add contact form
    @PostMapping("/process-contact")
    public String processContact(@ModelAttribute @Valid Contact contact,
                                 @RequestParam ("profileImage") @RequestPart MultipartFile file,
                                 Principal principal, HttpSession session){


        try {
            String userEmail = principal.getName();

            User user = this.userService.getUser(userEmail);

            contact.setUser(user);

            // processing and uploading file....
            saveContactImage(contact, file);

            user.getContacts().add(contact);

            this.userService.saveUser(user);

            System.err.println("ADDED : "+user.getContacts());

            //message success
            session.setAttribute("message", new Messages("Contact Added Successfully", "success"));


        }catch (Exception e){
            // message error
            session.setAttribute("message", new Messages("Something Went Wrong!!" +e.getMessage()+", Try again.", "danger"));
        }

        return "normal/add-contact";
    }

    // show contacts handler
    // Per page = 5 contact
    // current page = 0[page]
    @GetMapping("/show-contacts/{page}")
    public String showContact(@PathVariable Integer page, Model model, Principal principal){
        model.addAttribute("title", "Show Contacts");

        User user = userService.getUser(principal.getName());
        // current page
        // Contact per page
        Pageable pageRequest = PageRequest.of(page, 5);

        Page<Contact> contactsList = contactsRepository.findContactByUser(user.getId(), pageRequest);

        model.addAttribute("contacts", contactsList);
        model.addAttribute("currentPage", page);

        model.addAttribute("totalPages", contactsList.getTotalPages());


        return "normal/show-contacts";
    }


    @GetMapping("/{id}/contact")
    public String showContactDetails(@PathVariable Long id, Model model, Principal principal){
        System.err.println(id);
        model.addAttribute("title", "Contact Details");

        // fetching the contact from database
        Optional<Contact> optionalContact = contactsRepository.findById(id);
        Contact contact = optionalContact.get();

        User user = userService.getUser(principal.getName());
        if (contact.getUser().getId().equals(user.getId())){
            model.addAttribute("contact", contact);
        }

        System.err.println(contact.getName()+ " " + contact.getEmail());
        return "normal/contact-details";
    }

    // deleting contact
    @GetMapping("/delete-contact/{id}")
    public String deleteContact(@PathVariable Long id, HttpSession session, Principal principal){
        User user = userService.getUser(principal.getName());
        Optional<Contact> contactOptional = contactsRepository.findById(id);

        if (contactOptional.get().getUser().getId().equals(user.getId())){
            contactsRepository.deleteById(id);
            System.err.println("INSIDE delete method");
            session.setAttribute("message", new Messages("Contact Deleted Successfully", "success"));

            return "redirect:/user/show-contacts/0";
        }
        return "redirect:/user/show-contacts/0";
    }


    // Open update form handler
    @PostMapping("/update-contact/{id}")
    public String updateForm(@PathVariable Long id, Model model){
        model.addAttribute("title", "Update Contact");
        Contact contact = this.contactsRepository.findById(id).get();

        model.addAttribute("contact", contact);
        return "normal/update-form";
    }


    // Process the Update Contact
    @PostMapping("/process-update/{id}")
    public String updateContact(@PathVariable Long id, @ModelAttribute Contact newContact,
                                @RequestParam("profileImage") @RequestPart MultipartFile file,
                                Principal principal){

        Contact oldContact = this.contactsRepository.findById(id).get();



        User user = userService.getUser(principal.getName());
        newContact.setId(id);
        newContact.setUser(user);

        // processing and uploading file....
        try {
            if (file.isEmpty()){
                newContact.setImageUrl(oldContact.getImageUrl());
            }else{
                // Deleting the old file
                File deleteFile = new ClassPathResource("static/img").getFile();
                File file1 = new File(deleteFile, oldContact.getImageUrl());
                file1.delete();


                // Updating the new image
                saveContactImage(newContact, file);


            }
        }catch (Exception e){
            System.err.println(e.getMessage());
        }

        contactsRepository.save(newContact);
        return "redirect:/user/"+id+"/contact";
    }

    private void saveContactImage(@ModelAttribute Contact newContact,
                                  @RequestPart @RequestParam("profileImage") MultipartFile file) throws IOException {
        if (!file.isEmpty()) {

            newContact.setImageUrl(file.getOriginalFilename());

            File saveFile = new ClassPathResource("static/img").getFile();

            Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());


            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            System.err.println("Image is uploaded");
        }
    }
}
