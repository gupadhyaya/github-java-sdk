package com.github.api.v2.services.impl;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.github.api.v2.services.GitHubAPIResponse;

public class GitHubAPIResponseImpl implements GitHubAPIResponse {

	private InputStream inp;
	private Map<String, List<String>> headers;

	public GitHubAPIResponseImpl(InputStream inp, Map<String, List<String>> headers) {
		this.inp = inp;
		this.headers = headers;
	}

	@Override
	public InputStream getInputStream() {
		return inp;
	}

	/* (non-Javadoc)
	 * @see com.github.api.v2.services.GitHubAPIResponse#getHeader(java.lang.String)
	 * this method is kinda stupid right now because it always returns the first
	 * header for a header. It is VERY unlikely this will pose a problem in most cases.
	 */
	@Override
	public String getHeader(String header) {
		if (headers.containsValue(header)) {
			return headers.get(header).get(0);
		}
		return null;
	}

	@Override
	public Map<String, List<String>> getHeaders() {
		return headers;
	}

}
