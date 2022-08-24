package com.barclays.accountmanagement.serviceimpls;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.barclays.accountmanagement.services.AccCreationEmailService;

@Service
public class AccCreationEmailServiceImpI implements AccCreationEmailService {
	
	@Autowired
    private JavaMailSender mailSender;
	public void sendEmail(String emailUserName, String emailPassword, String customerEmail)
	{
		SimpleMailMessage message = new SimpleMailMessage();
        System.out.println(customerEmail);
        message.setFrom("acc.management.system@gmail.com");
        message.setTo(customerEmail);
        message.setText("ACCOUNT CREATION SUCCESSFULL!"
        		+ "\n\nPlease find your username and password here"
        		+ "\nChange your password in your first login"
        		+ "\nUserName: "+ emailUserName +"\nPassword: "+emailPassword);
        message.setSubject("User credentials");
        mailSender.send(message);
        System.out.println("Mail sent...");
	}

}
