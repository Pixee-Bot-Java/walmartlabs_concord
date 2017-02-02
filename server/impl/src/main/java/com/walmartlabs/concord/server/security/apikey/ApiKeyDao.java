package com.walmartlabs.concord.server.security.apikey;

import com.google.common.base.Throwables;
import com.walmartlabs.concord.common.db.AbstractDao;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Base64.Encoder;

import static com.walmartlabs.concord.server.jooq.public_.tables.ApiKeys.API_KEYS;

@Named
public class ApiKeyDao extends AbstractDao {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyDao.class);

    @Inject
    public ApiKeyDao(Configuration cfg) {
        super(cfg);
    }

    public String newApiKey() {
        byte[] ab = new byte[16];

        try {
            SecureRandom r = SecureRandom.getInstanceStrong();
            r.nextBytes(ab);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error while creating a new API token", e);
        }

        Encoder e = Base64.getEncoder().withoutPadding();
        return e.encodeToString(ab);
    }

    public void insert(String id, String userId, String key) {
        transaction(cfg -> {
            DSLContext create = DSL.using(cfg);
            create.insertInto(API_KEYS)
                    .columns(API_KEYS.KEY_ID, API_KEYS.USER_ID, API_KEYS.API_KEY)
                    .values(id, userId, hash(key))
                    .execute();
        });
        log.info("insert ['{}', '{}', '*******'] -> done", id, userId);
    }

    public void delete(String id) {
        transaction(cfg -> {
            DSLContext create = DSL.using(cfg);
            create.deleteFrom(API_KEYS)
                    .where(API_KEYS.KEY_ID.eq(id))
                    .execute();
        });
        log.info("delete ['{}'] -> done", id);
    }

    public String findUserId(String key) {
        try (DSLContext create = DSL.using(cfg)) {
            String id = create.select(API_KEYS.USER_ID)
                    .from(API_KEYS)
                    .where(API_KEYS.API_KEY.eq(hash(key)))
                    .fetchOne(API_KEYS.USER_ID);

            if (id == null) {
                log.debug("findUserId ['{}'] -> not found", key);
                return null;
            }

            log.debug("findUserId ['{}'] -> found: {}", key, id);
            return id;
        }
    }

    private static String hash(String s) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw Throwables.propagate(e);
        }

        byte[] ab = Base64.getDecoder().decode(s);
        ab = md.digest(ab);

        return Base64.getEncoder().withoutPadding().encodeToString(ab);
    }
}
