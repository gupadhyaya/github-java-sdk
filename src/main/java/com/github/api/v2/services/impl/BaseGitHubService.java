/*
 * Copyright 2010 Nabeel Mukhtar 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 * 
 */
package com.github.api.v2.services.impl;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.github.api.v2.schema.Discussion;
import com.github.api.v2.schema.Gist;
import com.github.api.v2.schema.Issue;
import com.github.api.v2.schema.Job;
import com.github.api.v2.schema.Language;
import com.github.api.v2.schema.Organization;
import com.github.api.v2.schema.Permission;
import com.github.api.v2.schema.Repository;
import com.github.api.v2.schema.SchemaEntity;
import com.github.api.v2.schema.Tree;
import com.github.api.v2.services.AsyncResponseHandler;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubService;
import com.github.api.v2.services.constant.ApplicationConstants;
import com.github.api.v2.services.constant.GitHubApiUrls.GitHubApiUrlBuilder;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

/**
 * The Class BaseGitHubService.
 */
public abstract class BaseGitHubService extends GitHubApiGateway implements GitHubService {
        
    /** The Constant UTF_8_CHAR_SET. */
    protected static final Charset UTF_8_CHAR_SET = Charset.forName(ApplicationConstants.CONTENT_ENCODING);
        
    /** The parser. */
    protected final JsonParser parser = new JsonParser();
    
    /** The handlers. */
    private List<AsyncResponseHandler<List<? extends SchemaEntity>>> handlers = new ArrayList<AsyncResponseHandler<List<? extends SchemaEntity>>>();
    
    /** Rate limiting stuff */
    private static final String HEADER_RATELIMIT = "X-RateLimit-Limit";
    private static final String HEADER_RATELIMITREMAINING = "X-RateLimit-Remaining";
    private Integer rateLimit = null;
    private Integer rateLimitRemaining = null;
    private Map<String, List<String>> headers = null;

    /**
     * Instantiates a new base git hub service.
     */
    public BaseGitHubService() {
        // by default we compress contents
        requestHeaders.put("Accept-Encoding", "gzip, deflate");
    }

    /**
     * Instantiates a new base git hub service.
     * 
     * @param apiVersion
     *            the api version
     */
    public BaseGitHubService(String apiVersion) {
        setApiVersion(apiVersion);
    }
        
    /**
     * Unmarshall.
     * 
     * @param typeToken
     *            the type token
     * @param response
     *            the response
     * 
     * @return the t
     */
    @SuppressWarnings("unchecked")
    protected <T> T unmarshall(TypeToken<T> typeToken, JsonElement response) {
    	// 2011-08-07T20:24:54-07:00
        Gson gson = getGsonBuilder().create();
        return (T) gson.fromJson(response, typeToken.getType());
    }

    /**
     * Notify observers.
     * 
     * @param response
     *            the response
     */
    protected void notifyObservers(List<? extends SchemaEntity> response) {
        for(AsyncResponseHandler<List<? extends SchemaEntity>> handler : handlers) {
            handler.handleResponse(response);
        }
    }
        
    /* (non-Javadoc)
     * @see com.google.code.stackexchange.client.query.StackExchangeApiQuery#addResonseHandler(com.google.code.stackexchange.client.AsyncResponseHandler)
     */
    /**
     * Adds the resonse handler.
     * 
     * @param handler
     *            the handler
     */
    public void addResonseHandler(AsyncResponseHandler<List<? extends SchemaEntity>> handler) {
        handlers.add(handler);
    }
        
    /**
     * Gets the gson builder.
     * 
     * @return the gson builder
     */
    protected GsonBuilder getGsonBuilder() {
        GsonBuilder builder = new GsonBuilder();
        builder.setDateFormat(ApplicationConstants.DATE_FORMAT);
        builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        builder.registerTypeAdapter(Issue.State.class, new JsonDeserializer<Issue.State>() {
                @Override
                public Issue.State deserialize(JsonElement arg0, Type arg1,
                                               JsonDeserializationContext arg2) throws JsonParseException {
                    return Issue.State.fromValue(arg0.getAsString());
                }
            });
        builder.registerTypeAdapter(Repository.Visibility.class, new JsonDeserializer<Repository.Visibility>() {
                @Override
                public Repository.Visibility deserialize(JsonElement arg0, Type arg1,
                                                         JsonDeserializationContext arg2) throws JsonParseException {
                    return (arg0.getAsBoolean())? Repository.Visibility.PRIVATE : Repository.Visibility.PUBLIC;
                }
            });
        builder.registerTypeAdapter(Gist.Visibility.class, new JsonDeserializer<Gist.Visibility>() {
                @Override
                public Gist.Visibility deserialize(JsonElement arg0, Type arg1,
                                                   JsonDeserializationContext arg2) throws JsonParseException {
                    return (arg0.getAsBoolean())? Gist.Visibility.PUBLIC : Gist.Visibility.PRIVATE;
                }
            });
        builder.registerTypeAdapter(Language.class, new JsonDeserializer<Language>() {
                @Override
                public Language deserialize(JsonElement arg0, Type arg1,
                                            JsonDeserializationContext arg2) throws JsonParseException {
                    return Language.fromValue(arg0.getAsString());
                }
            });
        builder.registerTypeAdapter(Tree.Type.class, new JsonDeserializer<Tree.Type>() {
                @Override
                public Tree.Type deserialize(JsonElement arg0, Type arg1,
                                             JsonDeserializationContext arg2) throws JsonParseException {
                    return Tree.Type.fromValue(arg0.getAsString());
                }
            });
        builder.registerTypeAdapter(Organization.Type.class, new JsonDeserializer<Organization.Type>() {
                @Override
                public Organization.Type deserialize(JsonElement arg0, Type arg1,
                                                     JsonDeserializationContext arg2) throws JsonParseException {
                    return Organization.Type.fromValue(arg0.getAsString());
                }
            });
        builder.registerTypeAdapter(Discussion.Type.class, new JsonDeserializer<Discussion.Type>() {
                @Override
                public Discussion.Type deserialize(JsonElement arg0, Type arg1,
                                                   JsonDeserializationContext arg2) throws JsonParseException {
                    return Discussion.Type.fromValue(arg0.getAsString());
                }
            });
        builder.registerTypeAdapter(Permission.class, new JsonDeserializer<Permission>() {
                @Override
                public Permission deserialize(JsonElement arg0, Type arg1,
                                              JsonDeserializationContext arg2) throws JsonParseException {
                    return Permission.fromValue(arg0.getAsString());
                }
            });
        builder.registerTypeAdapter(Job.Type.class, new JsonDeserializer<Job.Type>() {
                @Override
                public Job.Type deserialize(JsonElement arg0, Type arg1,
                                            JsonDeserializationContext arg2) throws JsonParseException {
                    return Job.Type.fromValue(arg0.getAsString());
                }
            });
        // added to handle different styles of dates
        builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
        return builder;
    }
    
    /**
     * Unmarshall.
     * 
     * @param jsonContent
     *            the json content
     * 
     * @return the json object
     */
    protected JsonObject unmarshall(InputStream jsonContent) {
        try {
            JsonElement element = parser.parse(new InputStreamReader(jsonContent, UTF_8_CHAR_SET));
            if (element.isJsonObject()) {
                return element.getAsJsonObject();
            } else {
                throw new GitHubException("Unknown content found in response." + element);
            }
        } catch (Exception e) {
            throw new GitHubException(e);
        } finally {
            closeStream(jsonContent);
        }
    }
        
    /**
     * Creates the git hub api url builder.
     * 
     * @param urlFormat
     *            the url format
     * 
     * @return the git hub api url builder
     */
    protected GitHubApiUrlBuilder createGitHubApiUrlBuilder(String urlFormat) {
        return new GitHubApiUrlBuilder(urlFormat);
    }

    /* (non-Javadoc)
     * @see com.github.api.v2.services.GitHubService#processHeaders(java.util.Map)
     */
    public void processHeaders(Map<String, List<String>> headers) {
	this.headers = headers;
	if (headers.containsKey(HEADER_RATELIMIT))
		rateLimit = Integer.parseInt(headers.get(HEADER_RATELIMIT).get(0));
	if (headers.containsKey(HEADER_RATELIMITREMAINING))
		rateLimitRemaining = Integer.parseInt(headers.get(HEADER_RATELIMITREMAINING).get(0));
    }

    /* (non-Javadoc)
     * @see com.github.api.v2.services.GitHubService#getRateLimit()
     */
    public int getRateLimit() {
	if (rateLimit == null) return -1;
	return rateLimit.intValue();
    }

    /* (non-Javadoc)
     * @see com.github.api.v2.services.GitHubService#getRateLimitRemaining()
     */
    public int getRateLimitRemaining() {
	if (rateLimitRemaining == null) return -1;
	return rateLimitRemaining.intValue();
    }
    
    /**
     * This brute forces a date. Needed because different elements of GitHub provide
     * dates in many different formats.
     * 
     * Based on code from the Caliper Project:
     *   http://code.google.com/p/caliper/source/browse/trunk/caliper/src/main/java/com/google/caliper/Json.java
     * @author patrick
     */
    private static class DateTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {
        private final DateFormat dateFormat;
        private final DateFormat altDateFormat;
        
        private DateTypeAdapter() {
        	// dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz", Locale.US);
        	dateFormat = new SimpleDateFormat(ApplicationConstants.DATE_FORMAT);
        	dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        	altDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        	altDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        @Override public synchronized JsonElement serialize(Date date, Type type,
            JsonSerializationContext jsonSerializationContext) {
        	return new JsonPrimitive(dateFormat.format(date));
        }

        @Override public synchronized Date deserialize(JsonElement jsonElement, Type type,
            JsonDeserializationContext jsonDeserializationContext) {
		    String dateString = jsonElement.getAsString();
		    // first try to parse as an ISO 8601 date
		    try {
		    	return dateFormat.parse(dateString);
		    } catch (ParseException ignored) {
		    }
		  
		    // next, try a GSON-style locale-specific dates
		    try {
		    	return DateFormat.getDateTimeInstance().parse(dateString);
		    } catch (ParseException ignored) {
		    }
		  
		    try {
		    	return altDateFormat.parse(dateString);
		    } catch (ParseException ignored) {
		    }
		  
		    try {
		    	return altDateFormat.parse(cleanDate(dateString));
		    } catch (ParseException ignored) {
		    }
		    
		    throw new JsonParseException(dateString);
        }
        
        protected String cleanDate(String inputDate) {
            return inputDate.replaceAll("([\\+\\-]\\d\\d):(\\d\\d)$", "$1$2");
        }
    }
}
