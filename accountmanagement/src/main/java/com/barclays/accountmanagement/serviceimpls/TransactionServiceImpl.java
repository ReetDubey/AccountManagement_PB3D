package com.barclays.accountmanagement.serviceimpls;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.barclays.accountmanagement.exception.InsufficientBalanceException;
import com.barclays.accountmanagement.exception.MaxWithdrawalLimitExceededException;
import com.barclays.accountmanagement.entity.Account;
import com.barclays.accountmanagement.entity.Customer;
import com.barclays.accountmanagement.entity.Transaction;
import com.barclays.accountmanagement.repositories.AccountRepo;
import com.barclays.accountmanagement.repositories.TransactionRepo;
import com.barclays.accountmanagement.services.TransactionService;

import java.util.List;


@Component
public class TransactionServiceImpl implements TransactionService {
		
	
	@Override
	public String Deposit(long depositID, double amount) {
	
		Account depositor=accountRepo.getReferenceById(depositID);
		
		Transaction transaction = new Transaction();
		transaction.setTransactionAmount(amount);
		depositor.setCurrentBalance(depositor.getCurrentBalance() + amount);
		transaction.setTransactionRefNum("T"+transaction.getTransactionId()+"-"+depositor.getAccountNumber());
		//transaction.setTransactionRefNum("T"+depositor.getAccountNumber()+String.format("%d",transaction.getTransactionId()));
		transaction.setDateTime(LocalDateTime.now());
		transaction.setTransactionType("Credit");
		transaction.setSubType("Deposit");
		

		
		depositor.getTransactions().add(transaction);
		
		//transactionRepo.save(transaction);
		accountRepo.save(depositor);
		GenerateTransactionRef(transaction,depositor);
		//sendEmail(depositor, transaction);	
		return "Transaction successful";
	}

	
	@Autowired
    private JavaMailSender mailSender;
//Email Module
	@Override
    public void sendEmail(Account account, Transaction transaction) {
        SimpleMailMessage message = new SimpleMailMessage();

        Customer customer = account.getCustomer();
        String emailId = customer.getEmail();
        System.out.println(emailId);
        message.setFrom("acc.management.system@gmail.com");
        message.setTo(emailId);
        
        String firstEncryptedAccountNumber = Long.toString(account.getAccountNumber()).substring(0, 3); 
        String lastEncryptedAccountNumber = Long.toString(account.getAccountNumber()).substring(5, 9);
        String encryptedAccountNumber = firstEncryptedAccountNumber +"XXX"+ lastEncryptedAccountNumber;
        
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
	    String now = dtf.format(transaction.getDateTime());  

	    String subjectStart = "Your account";
	    String subjectEnd;
	    
	    if(transaction.getTransactionType().equals("Credit")) {
	    	subjectEnd = "has been credited.";
	    }else {
	    	subjectEnd = "has been debited.";
	    }
	    
        message.setText("Transaction successful! \nTransaction reference number : " + transaction.getTransactionRefNum() +
        		"\nTransaction Date : " + now +
                "\nTransaction Type : " + transaction.getTransactionType() + 
        		"\nAmount transacted : $" + transaction.getTransactionAmount());
        message.setSubject(subjectStart + " " + encryptedAccountNumber + " " +subjectEnd);

        mailSender.send(message);
        System.out.println("Mail sent...");
    }
//Withdraw Module		
		@Override
		public Long getAmount() { 
			Long amt = (long) 100;
			return amt;
		}
		@Override
		public Long getCurrentBalance(long accountNumber) {
			Account account = accountRepo.getReferenceById(accountNumber);
			Long currentBalance = (long) account.getCurrentBalance();
			return currentBalance;
		}
	 
	@Override
	public Long DeductMoney(long accountNumber, Long amountToWithdraw) { 
		

			Account account = accountRepo.getReferenceById(accountNumber);
			
			if (! (checkLimit10000(accountNumber, amountToWithdraw)) ) {
				throw new MaxWithdrawalLimitExceededException();
			}
			
			Long currentBalance = getCurrentBalance(accountNumber);
		    Long newBalance = currentBalance - amountToWithdraw;
		    
			if(newBalance < 0 ) { 
				throw new InsufficientBalanceException(); }
			
			account.setCurrentBalance(newBalance);
			Transaction transaction = Transfer(amountToWithdraw, accountNumber);
			accountRepo.save(account);
			
			try {
				GenerateTransactionRef(transaction, account);
				//sendEmail(account, transaction);
			}catch( Exception e ){
				// catch error
				System.out.println("Error Sending Email: " + e.getMessage());
			}
			
		    return getCurrentBalance(accountNumber);
		}

	@Override
	public boolean checkLimit10000(long accountNumber, Long amountToWithdraw) {
			
			Account account = accountRepo.getReferenceById(accountNumber);
			List<Transaction> transactionList = account.getTransactions();
			
			int today = LocalDateTime.now().getDayOfMonth();
			
			double currentDailyLimit = 0;
			
			for(Transaction transaction : transactionList) {
				if(today == transaction.getDateTime().getDayOfMonth()) {
					if(transaction.getSubType().equals("Withdrawal"))
					{
						currentDailyLimit = currentDailyLimit + Math.abs(transaction.getTransactionAmount());
					}
				}
			}
			
			int parseCurrentDailyLimit = (int) currentDailyLimit;
			
			if(parseCurrentDailyLimit >= account.getDailyLimit()) {
				System.out.println("cannot withdraw");
				return false;
			}else if(account.getDailyLimit() - amountToWithdraw >= 0 ) {
				System.out.println("can withdraw");
				return true;
			}else {
				System.out.println("cannot withdraw1");
				return false;
			}
	}
	Transaction Transfer(double amountToWithdraw, long accountNumber) {
			
	    	Account account = accountRepo.getReferenceById(accountNumber);
	    	Transaction transaction = new Transaction();
			transaction.setTransactionAmount(-amountToWithdraw);
		//	transaction.setCurrentBalance(reciever.getCurrentBalance() + amount);
			transaction.setTransactionRefNum("T"+transaction.getDateTime().getDayOfMonth()
					+":"+transaction.getDateTime().getHour()+":"+transaction.getDateTime().getMinute()
					+"-"+account.getAccountNumber());
			transaction.setTransactionType("Debit");
			transaction.setSubType("Withdrawal");
			
			//transactionRepo.save(transaction);
			account.getTransactions().add(transaction);
			//accountRepo.save(account);
			return transaction;
	}
	

	
	@Autowired
	AccountRepo accountRepo;
	@Autowired
	TransactionRepo transactionRepo;
	
	@Override
	public String Transfer(long senderId, double amount, long recieverId) {
		
		Account sender = accountRepo.getReferenceById(senderId) ;
		Account reciever = accountRepo.getReferenceById(recieverId);
		
		if(sender.getCurrentBalance() < amount)
			return "Insufficient balance";
		if(amount < 0)
			return "Invalid amount";
		
		LocalDateTime timeStamp = LocalDateTime.now();
		
		Transaction senderTransaction = new Transaction();
		sender.setCurrentBalance(sender.getCurrentBalance() - amount);
		senderTransaction.setTransactionAmount(-amount);
		senderTransaction.setTransactionType("Debit");
		senderTransaction.setSubType("Transfer");
		senderTransaction.setDateTime(timeStamp);
		senderTransaction.setTransactionRefNum("T"+senderTransaction.getTransactionId()+"-"+sender.getAccountNumber()+"-"+reciever.getAccountNumber());
		sender.getTransactions().add(senderTransaction);
		accountRepo.save(sender);
		
		senderTransaction = GenerateRef(sender);
		int senderTID = senderTransaction.getTransactionId();
		//sendEmail(sender, senderTransaction);
		//transactionRepo.save(senderTransaction);
	
		Transaction recieverTransaction = new Transaction();
		reciever.setCurrentBalance(reciever.getCurrentBalance() + amount);
		recieverTransaction.setTransactionAmount(amount);
		recieverTransaction.setTransactionType("Credit");
		recieverTransaction.setSubType("Transfer");
		recieverTransaction.setDateTime(timeStamp);
		recieverTransaction.setTransactionRefNum("T"+recieverTransaction.getTransactionId()+"-"+sender.getAccountNumber()+"-"+reciever.getAccountNumber());
		reciever.getTransactions().add(recieverTransaction);
		accountRepo.save(reciever);	

		


		recieverTransaction = GenerateRef(reciever);
		int recieverTID = recieverTransaction.getTransactionId();
		
		recieverTransaction.setTransactionRefNum("T"+senderTID+"-"+recieverTID);
		senderTransaction.setTransactionRefNum("T"+senderTID+"-"+recieverTID);
		
		saveAndNotify(sender, senderTransaction);
		saveAndNotify(reciever, recieverTransaction);
			
		return "Successfull transfer";
		
		
		}
	
	@Override
	public List<Transaction> checkHistory(long accountNum) {
		Account account = accountRepo.getReferenceById(accountNum);
		List<Transaction> transactionList = account.getTransactions();
		
		
		int sizeOfList = transactionList.size();
		
		if(sizeOfList > 5)
		{
			System.out.println(transactionList.get(sizeOfList-1).toString());
			System.out.println(transactionList.get(sizeOfList-2).toString());
			System.out.println(transactionList.get(sizeOfList-3).toString());
			System.out.println(transactionList.get(sizeOfList-4).toString());
			System.out.println(transactionList.get(sizeOfList-5).toString());
//			return "Fetched history";
			return transactionList;
		}
		else {
			for(Transaction transaction : transactionList) {
				System.out.println(transaction.toString());
			}
//			transactionList.add(null);
//			return "Fetched history";
			return transactionList;
		}
		//return "Unable to fetch history";
	}
	void GenerateTransactionRef(Transaction transaction,Account account) {
		Page<Transaction> page = transactionRepo.findAll(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "transactionId")));
		transaction = page.getContent().get(0);
		transaction.setTransactionRefNum("T"+transaction.getTransactionId()+":"+account.getAccountNumber());
		//transaction.setTransactionId(transaction.getTransactionId());
		saveAndNotify(account, transaction);
	}
	Transaction GenerateRef(Account account) {
		List<Transaction> transactions = accountRepo.getReferenceById(account.getAccountNumber()).getTransactions();
		return transactions.get(transactions.size() - 1);
	}
	void saveAndNotify(Account account,Transaction transaction) {
		accountRepo.save(account);
		sendEmail(account, transaction);
	}


}
