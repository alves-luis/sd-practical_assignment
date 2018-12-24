/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cloudalloc;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 *
 * @author grupo
 */
public class User {

  private String email;
  private String password;
  private Map<String,Cloud> myClouds;
  private boolean loggedIn;
  private ReentrantLock lock;
  private Map<String,Condition> cloudExists; // associate a condition with each cloud
  private MessageLog log;
  private double debt;

  /**
   *
   * @param e
   * @param pass
   */
  public User(String e, String pass){
    this.email = e;
    this.password = pass;
    this.myClouds = new HashMap<>();
    this.loggedIn = true;
    this.lock = new ReentrantLock();
    this.cloudExists = new HashMap<>();
    this.log = new MessageLog();
    this.debt = 0;
  }

  /**
   *
   * @return
   */
  public String getEmail() {
    return email;
  }

  /**
   *
   * @param email
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   *
   * @param password
   */
  public void setPassword(String password) {
    this.password = password;
  }

  // Only one person can login

  /**
   *
   * @param pass
   * @return
   */
  public synchronized boolean login (String pass) {
    boolean canLogin = !loggedIn;
    loggedIn = this.password.equals(pass) && canLogin;
    return loggedIn;
  }

  /**
   * Only one person can logout at a time, that's why it is synchronized
   * @return
   */
  public synchronized boolean logout() {
    boolean canLogout = this.loggedIn;
    if (canLogout) this.loggedIn = false;
    return canLogout;
  }
  
  public synchronized boolean isLoggedIn() {
    return loggedIn;
  }

  /**
   *
   * @param c
   */
  public synchronized void addCloud(Cloud c) {
    this.myClouds.put(c.getId(),c);
    this.cloudExists.put(c.getId(),lock.newCondition());
  }

  /**
   *
   * @param id
   * @throws InexistentCloudException
   */
  public synchronized void removeCloud(String id) throws InexistentCloudException {
    Cloud c = this.myClouds.get(id);
    if (c == null)
      throw new InexistentCloudException(id);
    this.myClouds.remove(id);
    try {
      this.lock.lock();
      this.cloudExists.get(id).signal(); // sends notification that cloud is longer yours
    }
    finally {
      this.lock.unlock();
    }
    this.cloudExists.remove(id);
    debt += c.getAmmountToPay();
  }
  
  public synchronized boolean isMyCloud(String id) {
    return this.myClouds.containsKey(id);
  }

  /**
   *
   * @return
   */
  public synchronized double getTotalDebt() {
    return this.myClouds.values().stream().mapToDouble(c -> c.getAmmountToPay()).sum();
  }
  
  public double getDebt(String id) {
    Cloud c = this.myClouds.get(id);
    if (c == null)
      return 0;
    else
      return c.getAmmountToPay();
  }
  
  public double getDebt() {
    return this.debt;
  }
  
  public synchronized List<String> getCloudsId() {
    return this.myClouds.keySet().stream().collect(Collectors.toList());
  }
  
  public MessageLog getLog() {
    return this.log;
  }
  
  public void addMsg(String msg) {
    this.log.writeMessage(msg);
  }
  
  public String readMessage() {
    if (loggedIn)
      return log.readMessage();
    else
      return null;
  }

}
