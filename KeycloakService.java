import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import keycloak.migration.config.KeycloakConfig;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KeycloakService {

    public KeycloakService() {
    }
    public ResponseEntity<?> createRealm(String realmName, String realmId, boolean active) {
        String createRealmUrl = KeycloakConfig.KEYCLOAK_URL + "/admin/realms";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + this.extractTokens(this.getToken()).get("access_token"));
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestJson = "{ \"realm\": \"" + realmName + "\", \"id\": \"" + realmId + "\", \"enabled\": \"" + active + "\"}";
        HttpEntity<String> requestEntity = new HttpEntity<>(requestJson, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = new ResponseEntity<>("ERROR_CREATING_REALM",HttpStatus.NOT_FOUND);
        try{
            response = restTemplate.exchange(
                    createRealmUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            return response;
        }
        catch (Exception e){
            if(e.getMessage().toLowerCase().contains("conflict")){
                response = new ResponseEntity<>("REALM_NAME_ALREADY_EXIST",HttpStatus.CONFLICT);
            }else if(e.getMessage().toLowerCase().contains("unauthorized")){
                response = new ResponseEntity<>("UNAUTHORIZED_CLIENT",HttpStatus.UNAUTHORIZED);
            }
        }
        return response;
    }
    public ResponseEntity<?> createClient(Object client) {
        String url = KeycloakConfig.KEYCLOAK_URL + "/admin/realms/BCU_INT/clients";
        ResponseEntity<String> response = new ResponseEntity<>("ERROR_CREATING_CLIENT",HttpStatus.NOT_FOUND);
         RestTemplate restTemplate = new RestTemplate();
         ObjectMapper objectMapper = new ObjectMapper();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(this.extractTokens(this.getToken()).get("access_token"));
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(client);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("ERROR",HttpStatus.INTERNAL_SERVER_ERROR);
        }
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    public ResponseEntity<?> createUser(Object user) {
        String url = KeycloakConfig.KEYCLOAK_URL + "/admin/realms/BCU_INT/users";
        ResponseEntity<String> response = new ResponseEntity<>("ERROR_CREATING_USER",HttpStatus.NOT_FOUND);
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(this.extractTokens(this.getToken()).get("access_token"));
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(user);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("ERROR",HttpStatus.INTERNAL_SERVER_ERROR);
        }
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }
    public ResponseEntity<?> createRole(Object role,String clientId) {
        String url = KeycloakConfig.KEYCLOAK_URL + "/admin/realms/BCU_INT/clients/"+clientId+"/roles";
        System.out.println(role);
        ResponseEntity<String> response = new ResponseEntity<>("ERROR_CREATING_ROLE",HttpStatus.NOT_FOUND);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(this.extractTokens(this.getToken()).get("access_token"));
        String requestBody = role.toString();
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }
    public ResponseEntity<?> createGroup(Object group) {
        String url = KeycloakConfig.KEYCLOAK_URL + "/admin/realms/BCU_INT/groups";
        ResponseEntity<String> response = new ResponseEntity<>("ERROR_CREATING_GROUP",HttpStatus.NOT_FOUND);
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(this.extractTokens(this.getToken()).get("access_token"));
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(group);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("ERROR",HttpStatus.INTERNAL_SERVER_ERROR);
        }
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }
    public ResponseEntity<List<Object>> getRealmsFromKeycloak() {
        List<Object> realms = new ArrayList<>();
        ResponseEntity<List<Object>> response = new ResponseEntity<>(realms,HttpStatus.BAD_REQUEST);
        String endpointUrl = KeycloakConfig.KEYCLOAK_URL + "/admin/realms";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(this.extractTokens(this.getToken()).get("access_token"));
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseFromKeycloak = restTemplate.exchange(
                endpointUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        if(responseFromKeycloak.getStatusCode().is2xxSuccessful()){
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                String responseBody = responseFromKeycloak.getBody();
                JsonNode realmsJsonArray = objectMapper.readTree(responseBody);
                for (JsonNode realmNode : realmsJsonArray) {
                    realms.add(realmNode);
                }
                response = new ResponseEntity<>(realms,HttpStatus.OK);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            response = new ResponseEntity<>(realms,HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    public  String getToken() {
        String tokenUrl = KeycloakConfig.KEYCLOAK_URL + "/realms/master/protocol/openid-connect/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String requestBody = "grant_type=password&client_id=" + KeycloakConfig.ADMIN_CLIENT +
                "&username=" + KeycloakConfig.USERNAME +
                "&password=" + KeycloakConfig.PASSWORD;
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity;
        try {
            responseEntity = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                return responseEntity.getBody();
            } else {
                throw new RuntimeException("FAILED_TO_OBTAIN_TOKEN" + responseEntity.getStatusCodeValue());
            }
        } catch (Exception exception) {
            responseEntity = new ResponseEntity<>("SERVER_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
        return responseEntity.getBody();
    }
    public Map<String, String> extractTokens(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String accessToken = jsonNode.get("access_token").asText();
            String refreshToken = jsonNode.get("refresh_token").asText();
            Map<String, String> tokensMap = new HashMap<>();
            tokensMap.put("access_token", accessToken);
            tokensMap.put("refresh_token", refreshToken);
            return tokensMap;
        } catch (Exception e) {
            throw new RuntimeException("ERROR_DECODING_RESPONSE", e);
        }
    }
    public ResponseEntity<String> getClientFromKeycloak(String clientName) {
        String endpointUrl = KeycloakConfig.KEYCLOAK_URL + "/admin/realms/BCU_INT/clients?clientId="+clientName;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(this.extractTokens(this.getToken()).get("access_token"));
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseFromKeycloak = restTemplate.exchange(
                endpointUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        return  responseFromKeycloak;
    }
}
