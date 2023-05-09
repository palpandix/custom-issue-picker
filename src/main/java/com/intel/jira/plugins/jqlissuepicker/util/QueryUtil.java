package com.intel.jira.plugins.jqlissuepicker.util;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryUtil {
    private static final Logger LOG = LoggerFactory.getLogger(QueryUtil.class);
    private static Cache<String, Optional<SearchResults>> cache;

    private QueryUtil() {
        throw new AssertionError();
    }

    @Nonnull
    public static List<Issue> queryIssues(SearchService searchService, String jql, Issue issue, String jqlUser) {
        if (jql != null && !jql.isEmpty()) {
            SearchResults results = getCachedQueryResults(searchService, jql, jqlUser);
            if (results == null) {
                return Collections.emptyList();
            } else {
                List<Issue> issues = new ArrayList(results.getResults());
                issues.remove(issue);
                return issues;
            }
        } else {
            return Collections.emptyList();
        }
    }

    @Nullable
    private static SearchResults getCachedQueryResults(SearchService searchService, @Nonnull String jql, String jqlUser) {
        try {
            ApplicationUser user = getUser(jqlUser);
            String userName = user == null ? null : user.getName();
            LOG.trace("retrieving results jql {} as user {}", jql, userName);
            String key = userName + "|" + jql;
            Optional<SearchResults> result = (Optional)cache.get(key, () -> {
                LOG.trace("cache miss; calculating value");
                SearchResults searchResult = getQueryResults(searchService, jql, user);
                return Optional.ofNullable(searchResult);
            });
            return (SearchResults)result.orElse(null);
            //return (SearchResults)result.orElse((Object)null);
        } catch (ExecutionException var7) {
            LOG.error("could not get value from cache", var7);
            return null;
        }
    }

    private static SearchResults getQueryResults(SearchService searchService, String jql, ApplicationUser user) {
        LOG.debug("loading issues as user {} for JQL: {}", user == null ? null : user.getName(), jql);
        SearchService.ParseResult parseResult = searchService.parseQuery(user, jql);
        if (hasErrors(parseResult.getErrors())) {
            return null;
        } else {
            Query query = parseResult.getQuery();
            if (query == null) {
                return null;
            } else if (hasErrors(searchService.validateQuery(user, query))) {
                return null;
            } else {
                try {
                    SearchResults result = searchService.search(user, query, PagerFilter.getUnlimitedFilter());
                    LOG.debug("query returned {} issues", result.getTotal());
                    return result;
                } catch (SearchException var6) {
                    LOG.error("exception in search", var6);
                    return null;
                }
            }
        }
    }

    @Nullable
    public static ApplicationUser getUserForIndexing(String jqlUser) {
        if (StringUtils.isNotBlank(jqlUser)) {
            ApplicationUser user = ComponentAccessor.getUserManager().getUserByName(jqlUser);
            if (user != null) {
                return user;
            }

            LOG.warn("no such user: {}", jqlUser);
        }

        return null;
    }

    @Nullable
    public static ApplicationUser getUser(String jqlUser) {
        if (StringUtils.isNotBlank(jqlUser)) {
            ApplicationUser user = ComponentAccessor.getUserManager().getUserByName(jqlUser);
            if (user != null) {
                return user;
            }

            LOG.warn("no such user: {}", jqlUser);
        }

        return ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
    }

    private static boolean hasErrors(MessageSet messageSet) {
        boolean hasErrors = false;
        if (messageSet.hasAnyErrors()) {
            Collection messages = CollectionUtils.union(messageSet.getErrorMessages(), messageSet.getWarningMessages());

            for(Iterator var3 = messages.iterator(); var3.hasNext(); hasErrors = true) {
                Object message = var3.next();
                LOG.error("error in query: {}", message);
            }
        }

        return hasErrors;
    }

    static {
        cache = CacheBuilder.newBuilder().expireAfterWrite(60L, TimeUnit.SECONDS).build();
    }
}
