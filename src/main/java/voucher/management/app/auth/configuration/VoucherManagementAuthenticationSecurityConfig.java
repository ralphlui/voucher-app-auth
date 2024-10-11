package voucher.management.app.auth.configuration;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.HstsHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

@Configuration
@EnableWebSecurity
public class VoucherManagementAuthenticationSecurityConfig {
	private static final String[] SECURED_URLs = { "/api/**" };

	@Value("${aws.region}")
	private String awsRegion;

	@Value("${aws.accesskey}")
	private String awsAccessKey;

	@Value("${aws.secretkey}")
	private String awsSecretKey;

	@Value("${aws.ses.from}")
	private String emailFrom;

	@Value("${aws.sqs.url}")
	private String sqsURL;

	@Value("${frontend.url}")
	private String frontEndUrl;

	@Bean
	public String getEmailFrom() {
		return emailFrom;
	}

	@Bean
	public String getFrontEndUrl() {
		return frontEndUrl;
	}

	@Bean
	public String getSQSUrl() {
		return sqsURL;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	private static final String[] SECURED_URLS = { "/api/**" };
	
	@Bean
	public AWSCredentials awsCredentials() {
		return new BasicAWSCredentials(awsAccessKey, awsSecretKey);
	}
	

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
	    return http.cors(cors -> {
	        cors.configurationSource(request -> {
	            CorsConfiguration config = new CorsConfiguration();
	            config.setAllowedOrigins(List.of("*"));
	            config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "OPTIONS"));
	            config.setAllowedHeaders(List.of("*"));
	            config.applyPermitDefaultValues();
	            return config;
	        });
	    }).headers(headers -> headers
	        .addHeaderWriter(new StaticHeadersWriter("Access-Control-Allow-Origin", "*"))
	        .addHeaderWriter(new StaticHeadersWriter("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, OPTIONS"))
	        .addHeaderWriter(new StaticHeadersWriter("Access-Control-Allow-Headers", "*"))
	        .addHeaderWriter(new HstsHeaderWriter(31536000, false, true))
	        .addHeaderWriter((request, response) -> {
	            response.addHeader("Cache-Control", "max-age=60, must-revalidate");
	        })
	    ).csrf(AbstractHttpConfigurer::disable)
	    .authorizeHttpRequests(auth -> auth
	        .requestMatchers(SECURED_URLS).permitAll()
	        .anyRequest().authenticated()
	    )
	    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
	    .build();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}
	

	@Bean
	public AmazonSimpleEmailService sesClient() {
		AWSCredentials awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
		AmazonSimpleEmailService sesClient = AmazonSimpleEmailServiceClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).withRegion(awsRegion).build();
		return sesClient;
	}
	
	@Bean
    public AmazonSQS amazonSQSClient(AWSCredentials awsCredentials) {
        return AmazonSQSClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(awsRegion)
                .build();
    }

}
