package voucher.management.app.auth.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import voucher.management.app.auth.configuration.VoucherManagementAuthenticationSecurityConfig;
import voucher.management.app.auth.dto.AuditLogRequest;
import voucher.management.app.auth.service.IAuditService;

@Service
public class AuditLogService implements IAuditService {
	
	private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);
	
	
	@Autowired
	private VoucherManagementAuthenticationSecurityConfig securityConfig;

	@Async
    @Override
	public void sendAuditLogToSqs(String statusCode, String userId, String username, String activityType, String activityDescription,
			String requestActionEndpoint, String responseStatus, String requestType, String remarks) {
		try {
		    String auditLogRequest = createLogEntryRequest(statusCode, userId, username, activityType, activityDescription,
		        requestActionEndpoint, responseStatus, requestType, remarks);
		    
		    AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
		    String queueUrl = securityConfig.getSQSUrl();

		    SendMessageRequest sendMessageRequest = new SendMessageRequest()
		            .withQueueUrl(queueUrl)
		            .withMessageBody(auditLogRequest);

		    SendMessageResult sendMessageResult = sqs.sendMessage(sendMessageRequest);
		    logger.info("Message response in SQS: " + sendMessageResult.getMessageId());
		    
		} catch (Exception e) {
		    // Generic exception handling for any other unforeseen errors
		    logger.error("Exception: Unexpected error occurred while sending audit logs to SQS " + e.toString());
		}
	}
	
	
	private String createLogEntryRequest(String statusCode, String userId, String username, String activityType, String activityDescription,
			String requestActionEndpoint, String responseStatus, String requestType, String remarks) {
		
		ObjectMapper objectMapper = new ObjectMapper();
		AuditLogRequest logRequest = new AuditLogRequest(); 
		logRequest.setStatusCode(statusCode);
		logRequest.setUserId(userId);
		logRequest.setUsername(username);
		logRequest.setActivityType(activityType);
		logRequest.setActivityDescription(activityDescription);
		logRequest.setRequestActionEndpoint(requestActionEndpoint);
		logRequest.setResponseStatus(responseStatus);
		logRequest.setRequestType(requestType);
		logRequest.setRemarks(remarks);
		 try {
	            // Serialize Java object to JSON string
	            String auditLogString = objectMapper.writeValueAsString(logRequest);
	            logger.info("Serialized JSON: " + auditLogString);
	            return auditLogString;


	        } catch (Exception e) {
	        	logger.error("Exception: Unexpected error occurred while creatin audit log object", e.toString());
	            e.printStackTrace();
	        }
		 return "";
		
	}
	
}
