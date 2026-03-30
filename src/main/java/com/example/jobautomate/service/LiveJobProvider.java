package com.example.jobautomate.service;

import java.util.List;

public interface LiveJobProvider {

    boolean isConfigured();

    List<LiveJobPost> fetchJobs();
}
