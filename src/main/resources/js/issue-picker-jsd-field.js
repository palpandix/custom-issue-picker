AJS.toInit(function() {
    /**
     * Waits until selector is available under root.
     *
     * Remembers previous state of selector. Whenever the selector matches again, the callback is called.
     */
    function waitForElement(root, selector, callback) {
        var elementPresent = false;
        var observer = new MutationObserver(function(mutations, observer) {
            var result = AJS.$(selector);
            if (result.length > 0) {
                if (!elementPresent) {
                    elementPresent = true;
                    callback();
                }
            } else {
                elementPresent = false;
            }
        });
        observer.observe(root, { subtree: true, childList: true });
    }

    function fetchIssuePickerConfigurations(requestType) {
        var url = AJS.contextPath() +
            '/rest/ics-issuepicker/latest/issuepicker/configuration?requestType=' + encodeURIComponent(requestType);
        return AJS.$.get(url);
    }

    function fetchJsdViewData(issueKey) {
        var url = AJS.contextPath() +
            '/rest/ics-issuepicker/latest/issuepicker/jsd-viewdata?issueKey=' + encodeURIComponent(issueKey);
        return AJS.$.get(url);
    }

    function findIssueType(requestTypes, requestTypeId) {
        for (var i = 0; i < requestTypes.length; i++) {
            var type = requestTypes[i];
            if (type.id == requestTypeId) {
                return type.issueTypeId;
            }
        }
        return null;
    }

    function showErrorMessage(title, error) {
        AJS.log(title + ': ' + error);
    }

    function transformCustomField(id, config, requestTypeId) {
        // hide normal JSD field
        var selector = '#customfield_' + id;
        AJS.$(selector).prop('type', 'hidden');
        if (config.selectionMode === 'NONE') {
            AJS.$(selector).after('Table mode is not supported in Service Desk.');
            return;
        }
        // build select field
        var multiple = false;
        var select = ['<input id="', 'select_customfield_' + id, '" class="ics-select jsd-issue-picker'];
        if (config.selectionMode === 'MULTIPLE') {
            select.push(' ics-multi-select"');
            multiple = true;
        } else {
            select.push('" ');
        }
        select.push('>');
        AJS.$(selector).after(select.join(''));

        AJS.$('#select_customfield_' + id).auiSelect2({
            query: function queryIssuePicker(query) {
                var url = AJS.contextPath() +
                    '/rest/ics-issuepicker/latest/issuepicker/search?customFieldId=' + encodeURIComponent('customfield_' + id) +
                    '&requestType=' + encodeURIComponent(requestTypeId) +
                    '&query=' + encodeURIComponent(query.term);
                AJS.$.get(url).done(function(result) {
                    var data = { results: [] };
                    if (result) {
                        if (! multiple) {
                            data.results.push({ id: '', text: AJS.I18n.getText("common.words.none") });
                        }
                        result.issues.forEach(function (entry) {
                            data.results.push({ id: entry.key, text: entry.displayName });
                        });
                        if (result.issues.length < result.total) {
                            var message = AJS.I18n.getText("ics.issue-picker.more-results", result.issues.length, result.total);
                            data.results.push({ text: message });
                        }
                    }
                    query.callback(data);
                });
            },
            multiple: multiple,
            dropdownAutoWidth: true,
            placeholder: AJS.I18n.getText("ics.issue-picker.none")
        });
    }

    function transformViewCustomField(id, config) {
        var fieldName = config.serviceDeskFieldName;
        AJS.$('#content .cv-request-activity .request-fields dl').each(function() {
            var name = AJS.$(this).find('dt').text();
            if (name && name === fieldName) {
                var container = AJS.$(this).find('dd');
                container.html(config.currentIssueFieldValue);
            }
        });
    }

    // wait until the issue panel (.cv-request-create-container) is loaded.
    // since JSD switches between URLs without page reloads (it just replaces elements on the page)
    // the callback is called whenever the field #summary is re-added to the page
    waitForElement(document.getElementById('content'), '.cv-request-create-container', function() {
        // check if we are on the request creation page
        var parse = /.*\/customer\/portal\/(\d+)\/create\/(\d+)/.exec(location.pathname);
        if (!parse || !parse[1] || !parse[2]) {
            // these are not the pages you are looking for
            return;
        }

        // fetch field configurations for the current portal and issue type
        var requestType = parse[2];
        fetchIssuePickerConfigurations(requestType).done(function(configurationsResult) {
            var configurations = configurationsResult.configurations;
            Object.keys(configurations).forEach(function(customFieldId) {
                transformCustomField(customFieldId, configurations[customFieldId], requestType);
            });
            AJS.$('input.jsd-issue-picker').on('change', function(event) {
                var id = event.target.id;
                var value = AJS.$('#' + id).val();
                var originalId = id.substring('select_'.length);
                // copy value to original input field
                AJS.$('#' + originalId).val(value);
            });
        }).fail(function(xhr, status, error) {
            showErrorMessage('Error', xhr && xhr.responseText || error);
        });
    });

    waitForElement(document.getElementById('content'), '.cv-request-activity', function() {
        var parse = /.*\/customer\/portal\/(\d+)\/([^/]+-\d+)/.exec(location.pathname);
        if (parse && parse[1] && parse[2]) {
            fetchJsdViewData(parse[2]).done(function(configResult) {
                var configs = configResult.configurations;
                Object.keys(configs).forEach(function(customFieldId) {
                    transformViewCustomField(customFieldId, configs[customFieldId]);
                });
            });
        }
    });
});
