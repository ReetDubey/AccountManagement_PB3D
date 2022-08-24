package com.barclays.accountmanagement.serviceimpls;

import java.util.HashMap;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.barclays.accountmanagement.entity.User;
import com.barclays.accountmanagement.repositories.RoleRepository;
import com.barclays.accountmanagement.repositories.UserLoginRepository;
//import com.bank.accountmanagementsystem.serviceimpls.CustomPasswordEncoder;
import com.barclays.accountmanagement.services.UserLoginService;


@Service
public class UserLoginServiceImpl implements UserLoginService {
	
	@Autowired
	private RoleRepository roleRepository;
	
	@Autowired
	UserLoginRepository userLoginRepository;
	
	/**
	 * verifyUserCredential ( verify user name and pasword )
	 * @author khushbu mehta
	 */
	
	CustomPasswordEncoder passwordEncoder = new CustomPasswordEncoder();
	
	public ResponseEntity<Object> verifyUserCredential(String userId, String password, int roleId) {
		HashMap<String,String> result = new HashMap<String, String>();
		try {
			User user = userLoginRepository.findUserByUserIdAndPassword(userId, password);
			System.out.println("hello world");
			System.out.println(user.isFirstTime());
			User user1 = userLoginRepository.findUserByUserIdAndRoleRoleId(userId, roleId);
			
			if (user != null && user1 != null) {
				if(user.isFirstTime() == false) {
					result.put("message","Valid User");
					return new ResponseEntity<>(result,HttpStatus.OK);
				} else {
					result.put("message", "Please change your password");
					return new ResponseEntity<>(result, HttpStatus.UNAUTHORIZED);
				}
			} 
			else {
				result.put("message","Invalid User");
				return new ResponseEntity<>(result, HttpStatus.UNAUTHORIZED);
			}
		} catch(Exception e) {
			System.out.println("Invalid User");
			result.put("message","Invalid User");
			return new ResponseEntity<>(result, HttpStatus.UNAUTHORIZED);
		}
		

	}

	public User createNewUserCredentials() {
		String userId = RandomStringUtils.randomAlphabetic(7);
	    String password = RandomStringUtils.randomAlphabetic(8);
	    User newUser = new User(userId,password,roleRepository.findById(1));
		return newUser;
	}
	
	public ResponseEntity<Object> updateUserCredential(String userId, String password) {
		User user1 = userLoginRepository.findUserByUserId(userId);
		HashMap<String,String> result = new HashMap<String, String>();
		if (user1 == null){
			result.put("message","User Not Found");
			return new ResponseEntity<>(result,HttpStatus.NOT_FOUND);
		}
		else if(user1.isFirstTime()==true){
			user1.setPassword(password);
			user1.setFirstTime(false);
			userLoginRepository.save(user1);
			result.put("message","Credentials Updated");
			return new ResponseEntity<>(result,HttpStatus.OK);
		}
		else {
			result.put("message","You have already updated your password");
			return new ResponseEntity<>(result,HttpStatus.BAD_REQUEST);
		}
		
	}

}
