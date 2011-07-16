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
package com.github.api.v2.services;

import java.util.List;
import java.util.Map;

/**
 * The Interface GitHubService.
 */
public interface GitHubService extends GitHubAuthenticator {
	/**
	 * Process the headers returned from an API call
	 *
	 * @param headers
	 * 				mapping of headers to list of possible values
	 */
	public void processHeaders(Map<String, List<String>> headers);

	/**
	 * Gets the periodic rate limit for GitHub services
	 *
	 * I'm not certain if there is a way to tell when the rate limit will reset
	 * Sometimes it's a day, sometimes it's a minute.
	 *
	 * @return the periodic rate limit
	 */
	public int getRateLimit();

	/**
	 * Gets the remaining rate limit for the API
	 *
	 * @see com.github.api.v2.services.GitHubService#getRateLimit()
	 *
	 * @return the periodic rate limit remaining
	 */
	public int getRateLimitRemaining();
}
