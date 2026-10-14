package org.uma.evolver.meta.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Posts a JSON body and reads a JSON answer; shared by the clients of the model servers. */
final class HttpJsonClient {

  static final ObjectMapper JSON = new ObjectMapper();

  private final HttpClient http;
  private final Duration timeout;

  HttpJsonClient(Duration timeout) {
    this.timeout = timeout;
    this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  }

  /**
   * Posts a body and returns the parsed answer.
   *
   * @throws IOException if the server cannot be reached, does not answer in time, or answers with
   *     an HTTP error (the message has the status and the start of the body)
   */
  JsonNode post(URI uri, Object body, Map<String, String> headers) throws IOException {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(uri)
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
    headers.forEach(request::header);
    HttpResponse<String> response;
    try {
      response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while waiting for " + uri, e);
    }
    if (response.statusCode() / 100 != 2) {
      String text = response.body();
      throw new IOException(
          "HTTP "
              + response.statusCode()
              + " from "
              + uri
              + ": "
              + text.substring(0, Math.min(text.length(), 300)));
    }
    return JSON.readTree(response.body());
  }
}
