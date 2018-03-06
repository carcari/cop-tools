package eu.serco.tools.swift;

import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.client.factory.AccountFactory;
import org.javaswift.joss.client.factory.AuthenticationMethod;
import org.javaswift.joss.model.Account;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Scope(value = "singleton")
@Component
public class StorageAccount {


    @Value("${ovh.tenant.name}")
    private String tenantname;

    @Value("${ovh.tenant.id}")
    private String tenantid;

    @Value("${ovh.username}")
    private String ovhusername;

    @Value("${ovh.password}")
    private String ovhpassword;

    @Value("${ovh.auth.url}")
    private String authUrl;

    @Value("${ovh.region}")
    private String region;

    public Account createAccount() {
        AccountConfig config = new AccountConfig();
        config.setUsername( ovhusername);
        config.setPassword(ovhpassword);
        config.setAuthUrl(authUrl);
        config.setTenantId(tenantid);
        config.setTenantName(tenantname);
        config.setDisableSslValidation(false);
        config.setAuthenticationMethod(AuthenticationMethod.KEYSTONE);
        config.setPreferredRegion(region);
        return new AccountFactory(config).createAccount();
    }


}
