import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class AuthTestSupport6 {
    static final String PASSWORD="TestOnly6!";
    static String token(String html) {
        var m=java.util.regex.Pattern.compile("name=\"csrfToken\" value=\"([^\"]+)\"").matcher(html);
        if(!m.find())throw new AssertionError("Missing CSRF");return m.group(1);
    }
    static HttpClient client(){return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).followRedirects(HttpClient.Redirect.NEVER).build();}
    static HttpResponse<String> send(HttpClient client,String url,Map<String,String> data)throws Exception{
        StringJoiner body=new StringJoiner("&");data.forEach((k,v)->body.add(URLEncoder.encode(k,StandardCharsets.UTF_8)+"="+URLEncoder.encode(v,StandardCharsets.UTF_8)));
        return client.send(HttpRequest.newBuilder(URI.create(url)).header("Content-Type","application/x-www-form-urlencoded;charset=UTF-8").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    static HttpResponse<String> get(HttpClient c,String url)throws Exception{return c.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));}
    static void login(HttpClient client,String base,String username)throws Exception{
        String token=token(get(client,base+"/auth").body());
        HttpResponse<String> response=send(client,base+"/auth",Map.of("csrfToken",token,"username",username,"password",PASSWORD));
        if(response.statusCode()!=303)throw new AssertionError("Login failed: "+response.statusCode());
    }
    static void assertDeniedLogin(HttpClient client,String base,String username,String password)throws Exception{
        String token=token(get(client,base+"/auth").body());
        var response=send(client,base+"/auth",Map.of("csrfToken",token,"username",username,"password",password));
        if(response.statusCode()!=200 || !response.body().contains("Tên đăng nhập hoặc mật khẩu không đúng."))throw new AssertionError("Invalid login accepted");
    }
}
