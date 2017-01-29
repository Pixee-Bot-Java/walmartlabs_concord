package com.walmartlabs.concord.it.server;

import com.walmartlabs.concord.common.Constants;
import com.walmartlabs.concord.common.IOUtils;
import com.walmartlabs.concord.server.api.process.ProcessResource;
import com.walmartlabs.concord.server.api.process.ProcessStatus;
import com.walmartlabs.concord.server.api.process.ProcessStatusResponse;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.After;
import org.junit.Before;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.function.Supplier;
import java.util.zip.ZipOutputStream;

import static com.walmartlabs.concord.common.IOUtils.grep;
import static org.junit.Assert.assertEquals;

public abstract class AbstractServerIT {

    /**
     * As defined in db/src/main/resources/com/walmartlabs/concord/server/db/v0.0.1.xml
     */
    protected static final String DEFAULT_API_KEY = "auBy4eDWrKWsyhiDp3AQiw";

    private Client client;
    private WebTarget target;
    private String apiKey = DEFAULT_API_KEY;

    @Before
    public void _init() throws Exception {
        client = createClient(this::getApiKey);
        target = client.target(ITConstants.SERVER_URL);
    }

    @After
    public void _destroy() {
        if (client != null) {
            client.close();
        }
    }

    protected String getApiKey() {
        return apiKey;
    }

    protected void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    protected <T> T proxy(Class<T> klass) {
        return ((ResteasyWebTarget) target).proxy(klass);
    }

    protected static ProcessStatusResponse waitForCompletion(ProcessResource processResource, String instanceId) throws InterruptedException {
        int retries = 5;

        ProcessStatusResponse pir;
        while (true) {
            try {
                pir = processResource.get(instanceId);
                if (pir.getStatus() != ProcessStatus.RUNNING && pir.getStatus() != ProcessStatus.STARTING) {
                    break;
                }
            } catch (NotFoundException e) {
                System.out.println(String.format("waitForCompletion ['%s'] -> not found, retrying... (%s)", instanceId, retries));
                if (retries-- < 0) {
                    throw e;
                }
            }
            Thread.sleep(1000);
        }
        return pir;
    }

    protected static void assertLog(String pattern, byte[] ab) throws IOException {
        assertEquals(1, grep(pattern, ab).size());
    }

    protected byte[] getLog(ProcessStatusResponse pir) {
        WebTarget t = client.target(ITConstants.SERVER_URL + "/logs/" + pir.getLogFileName());
        Response resp = t.request().get();
        assertEquals(Status.OK.getStatusCode(), resp.getStatus());
        byte[] ab = resp.readEntity(byte[].class);
        resp.close();
        return ab;
    }

    protected static byte[] archive(URI uri) throws IOException {
        return archive(uri, null);
    }

    protected static byte[] archive(URI uri, String depsDir) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            IOUtils.zip(zip, Paths.get(uri));
            if (depsDir != null) {
                IOUtils.zip(zip, Constants.LIBRARIES_DIR_NAME + "/", Paths.get(depsDir));
            }
        }
        return out.toByteArray();
    }

    private static Client createClient(Supplier<String> k) {
        return ClientBuilder.newClient()
                .register((ClientRequestFilter) requestContext -> {
                    MultivaluedMap<String, Object> headers = requestContext.getHeaders();
                    headers.putSingle("Authorization", k.get());
                });
    }
}
