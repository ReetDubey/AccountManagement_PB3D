package com.barclays.accountmanagement.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.barclays.accountmanagement.entity.Account;
import com.barclays.accountmanagement.entity.Customer;
import com.barclays.accountmanagement.entity.User;
import com.barclays.accountmanagement.services.AccCreationEmailService;
import com.barclays.accountmanagement.services.ManagerService;
import com.barclays.accountmanagement.services.UserLoginService;
import com.barclays.accountmanagement.constants.SystemConstants;
/**
 * ManagerController contains all api endpoints for manager functionalities.
 *
 */
@RestController
public class ManagerController {

	@Autowired
	ManagerService managerService;

	@Autowired
	AccCreationEmailService accCreationEmailService;

	@Autowired
	UserLoginService userLoginService;

	/**
	 * create a new online account for customer
	 * @author dakshin
	 * 
	 * @param panCard
	 * @param aadharNumber
	 * @param name
	 * @param postalAddress
	 * @param email
	 * @param dob
	 * @param panImg
	 * @param aadharImg
	 * @return customerId
	 */
	@PostMapping(SystemConstants.CREATE_ONLINE_ACCOUNT)
	public ResponseEntity<Object> createCustomer(@RequestParam(value = "pan_number") String panCard,
			@RequestParam(value = "aadhar_number") long aadharNumber, @RequestParam(value = "name") String name,
			@RequestParam(value = "postal_address") String postalAddress, @RequestParam(value = "email") String email,
			@RequestParam(value = "dob") String dob, @RequestParam(value = "pan_img") MultipartFile panImg,
			@RequestParam(value = "aadhar_img") MultipartFile aadharImg) {

		Customer newCust = new Customer();
		HashMap<String, String> resultSet = new HashMap<String, String>();
		newCust.setName(name);
		if (!Pattern.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}", dob)) {
			resultSet.put("message", "Invalid DOB! Format should me YYYY-MM-DD");
			return new ResponseEntity<>(resultSet, HttpStatus.BAD_REQUEST);
		}
		newCust.setDobFromString(dob);
		if (!Pattern.matches("[A-Z]{5}[0-9]{4}[A-Z]{1}", panCard)) {
			resultSet.put("message", "Invalid PAN number");
			return new ResponseEntity<>(resultSet, HttpStatus.BAD_REQUEST);
		}
		newCust.setPanCard(panCard);
		if (!Pattern.matches("[0-9]{12}", Long.toString(aadharNumber))) {
			resultSet.put("message", "Invalid Aadhar number");
			return new ResponseEntity<>(resultSet, HttpStatus.BAD_REQUEST);
		}
		newCust.setAadharNumber(aadharNumber);
		newCust.setPostalAddress(postalAddress);
		if (!Pattern.matches("^\\w+([\\.-]?\\w+)*@\\w+([\\.-]?\\w+)*(\\.\\w{2,3})+$", email)) {
			resultSet.put("message", "Invalid email");
			return new ResponseEntity<>(resultSet, HttpStatus.BAD_REQUEST);
		}
		newCust.setEmail(email);
		User newUser = userLoginService.createNewUserCredentials();
		newCust.setUser(newUser);
		try {
			newCust.setAadharImg(aadharImg.getBytes());
			newCust.setPanImg(panImg.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
			resultSet.put("message", e.getMessage());
			return new ResponseEntity<>(resultSet, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		try {
			newCust = managerService.createNewCustomer(newCust);
		} catch (Exception e) {
			e.printStackTrace();
			resultSet.put("message", e.getMessage());
			return new ResponseEntity<>(resultSet, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		resultSet.put("customer_id", Long.toString(newCust.getCustomerId()));
		String emailUserName = newUser.getUserId();
		String emailPassword = newUser.getPassword();
		String customerEmail = newCust.getEmail();
		accCreationEmailService.sendEmail(emailUserName, emailPassword, customerEmail);
		return new ResponseEntity<>(resultSet, HttpStatus.OK);
	}

	/**
	 * Create savings account for a particular customer
	 * @author nishad
	 * 
	 * @param customerId
	 * @return accountNumber
	 */
	@PostMapping(SystemConstants.CREATE_SAVING_ACCOUNT)
	public ResponseEntity<Object> createAccount(@RequestParam(value = "customer_id") int customerId) {
		HashMap<String, String> resultSet = new HashMap<String, String>();
		try {
			Account account = managerService.createNewAccount(customerId);
			resultSet.put("customer_id", Integer.toString(customerId));
			resultSet.put("account_number", Long.toString(account.getAccountNumber()));
		} catch (Exception e) {
			e.printStackTrace();
			resultSet.put("message", e.getMessage());
			return new ResponseEntity<>(resultSet, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<>(resultSet, HttpStatus.OK);
	}

	/**
	 * @author
	 * 
	 * @param panCardNumber
	 * @return account already exists or not and customerId
	 */
	@GetMapping(SystemConstants.VERIFY_PANCARD)
	public ResponseEntity<Object> verifyIfPanCardExistss(@RequestParam(required = true) String panCardNumber) {
		return managerService.verifyPanCard(panCardNumber);
	}

	   
}