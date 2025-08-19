package com.BMS.Bank_Management_System.exception;


public class InsufficientBalanceException extends RuntimeException {
  public InsufficientBalanceException(String message) {
    super(message);
  }
}

