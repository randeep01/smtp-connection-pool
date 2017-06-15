package org.nlab.smtp.transport.connection;

import org.nlab.smtp.exception.MailSendException;
import org.nlab.smtp.pool.ObjectPoolAware;
import org.nlab.smtp.pool.SmtpConnectionPool;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.event.TransportListener;
import javax.mail.internet.MimeMessage;

/**
 * Created by nlabrot on 30/04/15.
 */
public class DefaultClosableSmtpConnection implements ClosableSmtpConnection, ObjectPoolAware<ClosableSmtpConnection> {

  private final Transport delegate;
  private SmtpConnectionPool objectPool;

  private final List<TransportListener> transportListeners = new ArrayList<>();

  public DefaultClosableSmtpConnection(Transport delegate) {
    this.delegate = delegate;
  }

  public void sendMessage(MimeMessage msg, Address[] recipients) throws MessagingException {
    doSend(msg, recipients);
  }

  public void sendMessage(MimeMessage msg) throws MessagingException {
    doSend(msg, msg.getAllRecipients());
  }

  public void sendMessages(MimeMessage... msgs) throws MailSendException {
    doSend(msgs);
  }

  public void addTransportListener(TransportListener l) {
    transportListeners.add(l);
    delegate.addTransportListener(l);
  }

  public void removeTransportListener(TransportListener l) {
    transportListeners.remove(l);
    delegate.removeTransportListener(l);
  }


  public void clearListeners() {
    for (TransportListener transportListener : transportListeners) {
      delegate.removeTransportListener(transportListener);
    }
    transportListeners.clear();
  }

  public boolean isConnected() {
    return delegate.isConnected();
  }


  @Override
  public void close() throws Exception {
    objectPool.returnObject(this);
  }

  @Override
  public SmtpConnectionPool getObjectPool() {
    return objectPool;
  }

  @Override
  public void setObjectPool(SmtpConnectionPool objectPool) {
    this.objectPool = objectPool;
  }

  @Override
  public Transport getDelegate() {
    return delegate;
  }

  @Override
  public Session getSession() {
    return objectPool.getSession();
  }


  private void doSend(MimeMessage mimeMessage, Address[] recipients) throws MessagingException {

    if (mimeMessage.getSentDate() == null) {
      mimeMessage.setSentDate(new Date());
    }
    String messageId = mimeMessage.getMessageID();
    mimeMessage.saveChanges();
    if (messageId != null) {
      // Preserve explicitly specified message id...
      mimeMessage.setHeader(HEADER_MESSAGE_ID, messageId);
    }
    delegate.sendMessage(mimeMessage, recipients);
  }


  private void doSend(MimeMessage... mimeMessages) throws MailSendException {
    Map<Object, Exception> failedMessages = new LinkedHashMap<>();

    for (int i = 0; i < mimeMessages.length; i++) {

      // Send message via current transport...
      MimeMessage mimeMessage = mimeMessages[i];
      try {
        doSend(mimeMessage, mimeMessage.getAllRecipients());
      } catch (Exception ex) {
        failedMessages.put(mimeMessage, ex);
      }
    }

    if (!failedMessages.isEmpty()) {
      throw new MailSendException(failedMessages);
    }
  }

}
