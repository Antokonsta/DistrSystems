package ru.sbt.ds;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Created by Anton on 12.05.17.
 */

@Service
public class RedirectFilter extends ZuulFilter {
    @Autowired
    private DiscoveryClient discoveryClient;

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 10;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        System.out.println("Filter is used!!");
        RequestContext context = RequestContext.getCurrentContext();

        String serviceId = context.getRequest().getRequestURI().split("/")[1];
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);

        int maxVersion = 0;
        URL targetServiceUrl = null;

        for (ServiceInstance instance : instances) {
            Map<String, String> metadata = instance.getMetadata();
            String deployment = metadata.get("deployment");
            for (Map.Entry entry : metadata.entrySet()) {
                //когда зеленая запускаем приложение
                if (entry.getKey().equals("version") && deployment.equals("green")) {
                    String version = (String) entry.getValue();
                    if (Integer.parseInt(version) > maxVersion) {
                        maxVersion = Integer.parseInt(version);
                        try {
                            targetServiceUrl = instance.getUri().toURL();
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        if (targetServiceUrl != null) {
                context.setRouteHost(targetServiceUrl);
        }
        return null;
    }
}
