package com.github.irybov.account;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.data.util.Streamable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.github.irybov.shared.AccountDTO;
import com.github.irybov.shared.BillDTO;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {
	
	private final TokenConfig tokenConfig;
//	private final Environment env;
//	@Value("${TOKEN_SECRET_FILE:/run/secrets/token_secret}")
//	private String secretFile;
//	private String secret;
//	private long tokenLifetimeSeconds;
//    private final Validator validator;
    private final BillClient billClient;
//	private final RestTemplate restTemplate;
	private final AccountMapper mapStruct;
	private final AccountJDBC jdbc;
	private final TransactionOperations txOps;
	
	@Lazy
	@Autowired
	private AccountService self;
	private static final String ACCOUNTS = "accounts";
/*	
	@PostConstruct
	void init() {
		try {
			this.secret = Files.readString(Path.of(secretFile)).stripTrailing();
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to read token secret from " + secretFile, e);
		}
		if (this.secret.isBlank()) {
			throw new IllegalStateException("Token secret file is empty: " + secretFile);
		}
		
		String tokenLifetime = env.getRequiredProperty("token.lifetime");
		this.tokenLifetimeSeconds = Long.parseLong(tokenLifetime);
		if (this.tokenLifetimeSeconds <= 0) {
			throw new IllegalStateException("token.lifetime must be positive, got: " + this.tokenLifetimeSeconds);
		}
	}
*/	
	@Transactional
	public void create(Registration registration) {
		
//		Set<ConstraintViolation<Registration>> violations = validator.validate(registration);
//		if(violations.isEmpty()) {
			Account account = mapStruct.toDB(registration);
//			account.setCreatedAt(Timestamp.valueOf(OffsetDateTime.now()
//				.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()));
//			account.setActive(true);
//			account.setRoles(Collections.singleton(Role.CLIENT.getName()));
			self.update(account);
//		}
	};
	
	@Transactional(readOnly = true, noRollbackFor = Exception.class)
	public String generateToken(String header) {
/*		
		String secret = null;
		try {
			secret = Files.readString(Path.of(secretFile)).stripTrailing();
		} catch (IOException e) {
			e.printStackTrace();
		}
*/		
		String[] login = header.split(":");
		Account account = self.getAccount(login[0]);
		if(account.getPassword().equals(login[1])) {
//			SecretKey key = new SecretKeySpec(env.getProperty("token.secret").getBytes(), 
//			SecretKey key = new SecretKeySpec(secret.getBytes(), SignatureAlgorithm.HS256.getJcaName());
			SecretKey key = Keys.hmacShaKeyFor(tokenConfig.getSecret().getBytes());
//			SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
			Instant now = Instant.now();
			String token = Jwts.builder()
					.claim("scope", account.getRoles())
					.claim("active", account.isActive())
					.subject(account.getPhone())
					.issuer("bankdemo")
//					.expiration(Date.from(now.plusSeconds(this.tokenLifetimeSeconds)))
					.expiration(Date.from(now.plusSeconds(tokenConfig.getTokenLifetimeSeconds())))
					.issuedAt(Date.from(now))
					.signWith(key, Jwts.SIG.HS256)
					.compact();
			return token;
		}
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong password provided");
	};
	
//	public AccountDTO getOne(int id) {return mapStruct.toDTO(jdbc.findById(id).get());}
	
	@Transactional(readOnly = true, noRollbackFor = Exception.class)
	public AccountDTO getOne(String phone) {
		
		Account account = self.getAccount(phone);
//		List<BillDTO> bills = billClient.getList(account.getId());
//		Set<BillDTO> bills = self.getList(account.getId());
		BillsResult billsResult = self.getList(account.getId());
//		ResponseEntity<List<BillDTO>> response = 
//				restTemplate.exchange("http://BILL/bills/" + account.getId() + "/list", 
//				HttpMethod.GET, null, new ParameterizedTypeReference<List<BillDTO>>(){});
		
		AccountDTO dto = mapStruct.toDTO(account);
//		dto.setBills(new HashSet<>(bills));
	    dto.setBills(billsResult.bills());
	    dto.setBillsUnavailable(billsResult.unavailable());
//		dto.setBills(bills);
		return dto;
	}

    @Cacheable(cacheNames = "sets", key = "#owner", unless = "#result.unavailable")
//    public Set<BillDTO> getList(int owner) {return self.fetchList(owner);}
    public BillsResult getList(int owner) {return self.fetchList(owner);}
    @CircuitBreaker(name = "bill", fallbackMethod = "emptyList")
//    public Set<BillDTO> fetchList(int owner) {return billClient.getList(owner);}
    public BillsResult fetchList(int owner) {return BillsResult.success(billClient.getList(owner));}
//    private Set<BillDTO> emptyList(int owner, Throwable exception) {return new LinkedHashSet<>();}
    private BillsResult emptyList(int owner, Throwable exception) {
//        if (exception instanceof ResponseStatusException ex && ex.getStatusCode().is4xxClientError()) {
//            throw ex;
//        }
        return BillsResult.fallback();
    }
	
	@Transactional(readOnly = true, noRollbackFor = Exception.class)
	public List<AccountDTO> getAll() {return mapStruct.toList(Streamable.of(jdbc.findAll()).toList());}
	
	@Transactional
	public void changePassword(String phone, String password) {
		
		Account account = self.getAccount(phone);
		account.setPassword(password);
		self.update(account);
	}
	
//	@Transactional
	public BillDTO addBill(String phone, String currency) {
		
		Account account = txOps.execute(status -> {
			try {return self.getAccount(phone);}
			catch(Exception e) {throw new RuntimeException(e);}
		});
		
		BillDTO bill = billClient.create(currency, account.getId());
//        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("http://BILL/bills")
//    	        .queryParam("currency", currency)
//    	        .queryParam("owner", account.getId());
//        BillDTO bill = restTemplate.postForObject(uriBuilder.toUriString(), null, BillDTO.class);
        
        if(account.getBills() != null) {account.getBills().add(bill.getId());}
        else {account.setBills(Stream.of(bill.getId()).collect(Collectors.toSet()));}
        
		txOps.executeWithoutResult(status -> {
			try {self.update(account);}
			catch(Exception e) {throw new RuntimeException(e);}
		});
		
        return bill;
	}
	
//	@Transactional
	public void deleteBill(String phone, int id) {
		
		Account account = txOps.execute(status -> {
			try {return self.getAccount(phone);}
			catch(Exception e) {throw new RuntimeException(e);}
		});
		
		billClient.delete(id);
		account.getBills().remove(id);
		
		txOps.executeWithoutResult(status -> {
			try {self.update(account);}
			catch(Exception e) {throw new RuntimeException(e);}
		});
	}
	
	@Transactional(propagation = Propagation.MANDATORY, noRollbackFor = Exception.class)
	@Cacheable(cacheNames = ACCOUNTS, key = "#phone", sync = true)
	public Account getAccount(String phone) {
		
		Account account = jdbc.findByPhone(phone);
		if(account != null) return account;
		throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
				String.format("Account with phone %s not found ", phone));
	}
	
	@Transactional(propagation = Propagation.MANDATORY)
	@CachePut(cacheNames = ACCOUNTS, key = "#result.phone", unless = "#result == null")
	public Account update(Account account) {
		jdbc.save(account);
		return account;
	}
/*	
	boolean checkFraud(String phone, String header) {
		
		String jwt = header.replace("Bearer", "").trim();
		
		String tokenSecret = env.getProperty("token.secret");
		byte[] secretKeyBytes = tokenSecret.getBytes();
		SecretKey secretKey = Keys.hmacShaKeyFor(secretKeyBytes);
		
		JwtParser parser = Jwts.parser()
                .verifyWith(secretKey)
                .build();
		Jws<Claims> parsedToken = 
				parser.parseSignedClaims(jwt);		
		String owner = parsedToken.getPayload().getSubject();
		
		if(owner.equals(phone)) return true;
		return false;
	}
*/	
}
