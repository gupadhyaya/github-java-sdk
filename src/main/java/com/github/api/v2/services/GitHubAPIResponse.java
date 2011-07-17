package com.github.api.v2.services;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface GitHubAPIResponse {
	public InputStream getInputStream();

	public String getHeader(String header);

	public Map<String, List<String>> getHeaders();
}
