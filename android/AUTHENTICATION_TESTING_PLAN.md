# Authentication Testing Plan - Reference Implementation Compatibility

## Overview
This document outlines the comprehensive testing plan to verify that our Android ANL implementation follows the exact same authentication sequence as the Windows ABClient reference implementation.

## Authentication Flow Testing

### 1. **Reference Authentication Sequence**

Based on the reference AuthManager implementation, the authentication flow should follow these exact steps:

#### **Step 1: Main Page Request**
```kotlin
// Expected Request
GET http://www.neverlands.ru/
Headers:
  - User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
  - Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
  - Accept-Language: ru-RU,ru;q=0.9,en;q=0.8
  - Accept-Encoding: gzip, deflate
  - Connection: keep-alive

// Expected Response
Status: 200 OK
Content: HTML page with auth form
```

#### **Step 2: Login Form Submission**
```kotlin
// Expected Request  
POST http://www.neverlands.ru/game.php
Headers:
  - User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
  - Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
  - Accept-Language: ru-RU,ru;q=0.9,en;q=0.8
  - Accept-Encoding: gzip, deflate
  - Connection: keep-alive
  - Referer: http://www.neverlands.ru/
  - Content-Type: application/x-www-form-urlencoded

Body (windows-1251 encoded):
player_nick=[encoded_username]&player_password=[encoded_password]
```

#### **Step 3: Response Analysis**
The system should analyze the game.php response for:
- **Success**: HTML5 game content or Flash player requirement
- **Captcha Required**: "Введите код" text and captcha image
- **Error**: "Неверный логин или пароль" or other error messages

#### **Step 4: Captcha Handling (if required)**
```kotlin
// Captcha Image Request
GET http://www.neverlands.ru/[captcha_image_url]
Headers: [same as above]

// Captcha Submission  
POST http://www.neverlands.ru/game.php
Headers: [same as above]
Body: player_nick=[encoded_username]&player_password=[encoded_password]&captcha=[captcha_code]
```

#### **Step 5: Flash Password (if required)**
```kotlin
// Flash Password Submission
POST http://www.neverlands.ru/game.php  
Headers: [same as above]
Body: flcheck=[encoded_flash_password]&nid=[player_id]
```

#### **Step 6: Session Verification**
```kotlin
// Session Check
GET http://www.neverlands.ru/main.php
Headers: [same as above]

// Expected: Game content without "Вход" or "Авторизация" text
```

## Test Cases

### **Test Case 1: Successful Login (No Captcha, No Flash)**
**Objective**: Verify basic authentication flow
**Steps**:
1. Create test profile with valid credentials
2. Execute login through AuthManager
3. Verify request sequence matches reference
4. Confirm successful authentication

**Expected Results**:
- All HTTP requests match reference headers and format
- Authentication succeeds without additional steps
- Session is established successfully

### **Test Case 2: Captcha Required Login**
**Objective**: Verify captcha handling integration
**Steps**:
1. Trigger captcha requirement (invalid attempts or server requirement)
2. Verify captcha image download
3. Simulate captcha code input
4. Verify captcha submission request format

**Expected Results**:
- Captcha image URL extraction works correctly
- Image downloads successfully as ByteArray
- CaptchaDialog displays properly
- Captcha submission follows reference format

### **Test Case 3: Flash Password Required**
**Objective**: Verify Flash game authentication
**Steps**:
1. Use profile that requires Flash password
2. Verify Flash player ID extraction from response
3. Simulate Flash password submission
4. Confirm game access

**Expected Results**:
- Flash player ID pattern matching works
- Flash password form submission follows reference format
- Game loads successfully after Flash authentication

### **Test Case 4: Authentication Errors**
**Objective**: Verify error handling matches reference
**Steps**:
1. Test invalid credentials
2. Test network errors
3. Test server errors
4. Verify error message extraction

**Expected Results**:
- Error patterns match reference implementation
- Error messages are properly extracted and displayed
- System handles failures gracefully

### **Test Case 5: Cookie Management**
**Objective**: Verify cookie handling compatibility
**Steps**:
1. Monitor cookie set/get operations during authentication
2. Verify NeverNick cookie handling
3. Test cookie persistence across requests
4. Verify domain/path settings

**Expected Results**:
- All cookies from server are properly stored
- Cookie format matches reference expectations
- NeverNick cookie validation works correctly
- Cookies persist properly across app restarts

## Automated Testing Implementation

### **Network Traffic Validation**
```kotlin
@Test
fun testAuthenticationSequence() {
    // Setup test profile
    val testProfile = UserProfile.createNew("testuser").copy(
        userPassword = "testpass"
    )
    
    // Mock network interceptor to capture requests
    val requestCapture = mutableListOf<RequestInfo>()
    
    // Execute authentication
    authManager.login(testProfile, authCallback)
    
    // Verify request sequence
    assertEquals(2, requestCapture.size) // Main page + login
    
    // Verify first request (main page)
    val mainPageRequest = requestCapture[0]
    assertEquals("GET", mainPageRequest.method)
    assertEquals("http://www.neverlands.ru/", mainPageRequest.url)
    assertEquals(EXPECTED_USER_AGENT, mainPageRequest.headers["User-Agent"])
    
    // Verify second request (login)
    val loginRequest = requestCapture[1]
    assertEquals("POST", loginRequest.method)
    assertEquals("http://www.neverlands.ru/game.php", loginRequest.url)
    assertTrue(loginRequest.body.contains("player_nick="))
    assertTrue(loginRequest.body.contains("player_password="))
}
```

### **Integration Testing with Real Server**
```kotlin
@Test
fun testRealServerAuthentication() {
    // Use test credentials against real server
    // Verify complete authentication flow
    // Check for regressions in server compatibility
}
```

## Manual Testing Checklist

### **Pre-Test Setup**
- [ ] Enable network logging in app
- [ ] Clear all cookies and app data
- [ ] Prepare test credentials
- [ ] Enable detailed debugging output

### **Authentication Flow Test**
- [ ] Open app and navigate to profiles
- [ ] Create new profile with test credentials
- [ ] Enable network traffic monitoring
- [ ] Attempt login and monitor network requests
- [ ] Verify all requests match reference format
- [ ] Check response handling for each step

### **Error Scenarios**
- [ ] Test with invalid credentials
- [ ] Test with network connectivity issues
- [ ] Test with server maintenance responses
- [ ] Verify error messages display correctly

### **Advanced Features**
- [ ] Test AutoLogin functionality
- [ ] Test profile management (create/edit/delete)
- [ ] Test proxy settings if configured
- [ ] Test password encryption/decryption

## Performance Benchmarks

### **Response Time Targets**
- Main page load: < 2 seconds
- Login submission: < 3 seconds  
- Captcha image load: < 1 second
- Overall authentication: < 10 seconds

### **Memory Usage**
- Authentication process should not cause memory leaks
- Image loading should be efficient
- Network buffers should be properly released

## Regression Testing

### **Compatibility Matrix**
Test authentication against:
- Different Android versions (API 24+)
- Various network conditions (WiFi, Mobile, Slow)
- Different server response variations
- Edge cases (empty responses, malformed HTML)

## Success Criteria

### **Core Requirements**
1. ✅ All HTTP requests match reference implementation format
2. ✅ Authentication sequence is identical to Windows version
3. ✅ Error handling matches reference behavior
4. ✅ Cookie management is compatible
5. ✅ Captcha integration works correctly
6. ✅ Flash password handling functions properly

### **Performance Requirements**
1. Authentication completes within acceptable time limits
2. Memory usage remains stable during process
3. Network requests are efficient and properly timed

### **User Experience**
1. All error messages are clear and helpful
2. Progress indicators work correctly
3. Authentication state is properly maintained
4. UI remains responsive during network operations

## Conclusion

This comprehensive testing plan ensures that our Android ANL implementation maintains 100% compatibility with the Windows ABClient reference implementation while providing the enhanced functionality and modern architecture that our implementation offers.

The testing should be performed both automated and manually to catch any edge cases or regressions that might affect compatibility with the game server.