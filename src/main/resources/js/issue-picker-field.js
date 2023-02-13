AJS.toInit(function() {
    function searchIssuePicker(id, issueId, currentProjectId, currentIssueTypeId, cfConfigId, queryTerm, resultCallback) {
        var url = AJS.contextPath() +
            '/rest/cwx-issuepicker/latest/issuepicker/search?customFieldId=' + encodeURIComponent(id);
        if (issueId) {
            url += '&issue=' + encodeURIComponent(issueId);
        } else {
            url += '&project=' + encodeURIComponent(currentProjectId) +
                '&issueType=' + encodeURIComponent(currentIssueTypeId) +
                '&cfConfigId=' + encodeURIComponent(cfConfigId);
        }
        url += '&query=' + encodeURIComponent(queryTerm);
        AJS.$.get(url).done(resultCallback);
    }

    function buildQueryIssuePickerFunction(id, issueId, currentProjectId, currentIssueTypeId, cfConfigId, multiple) {
        return function queryIssuePicker(query) {
            searchIssuePicker(id, issueId, currentProjectId, currentIssueTypeId, cfConfigId, query.term, function(result) {

                var data = { results: [] };
                if (result) {
                    if (! multiple) {
                        data.results.push({ id: '', text: AJS.I18n.getText("common.words.none") });
                    }
                    result.issues.forEach(function (entry) {
                        data.results.push({ id: entry.key, text: entry.displayName });
                    });
                    if (result.issues.length < result.total) {
                        var message = AJS.I18n.getText("cwx.issue-picker.more-results", result.issues.length, result.total);
                        data.results.push({ text: message });
                    }
                }
                query.callback(data);
            });
        };
    }

    function activateSelect(select) {
        var id = AJS.$(select).attr('id');
        var selectInput$ = AJS.$('#' + id + '-info');
        var createNewFieldId = selectInput$.data('create-new-field');
        var issueId = selectInput$.data('issue-id');
        var currentProjectId = selectInput$.data('current-project-id');
        var currentIssueTypeId = selectInput$.data('current-issue-type-id');
        var cfConfigId = selectInput$.data('cf-config-id');
        // to boolean
        var multiple = !!(selectInput$.data('multiple'));
        var presetValue = !!(selectInput$.data('preset-value'));
        var queryIssuePicker = buildQueryIssuePickerFunction(id, issueId, currentProjectId, currentIssueTypeId, cfConfigId, multiple);
        var createSearchChoice = null;
        if (createNewFieldId) {
            createSearchChoice = function createSearchChoice(term) {
                var encodedTerm = encodeURIComponent(term);
                return { id: 'new-' + encodedTerm, text: term + ' (new)' };
            };
        }
        AJS.$('#' + id).auiSelect2({
            createSearchChoice: createSearchChoice,
            query: queryIssuePicker,
            multiple: multiple,
            dropdownAutoWidth: true,
            placeholder: AJS.I18n.getText("cwx.issue-picker.none"),
            containerCssClass: 'long-field'
        });
        AJS.$('#' + id + '-popup-trigger').on('click', function() {
            var popupSearch = function(queryTerm, resultCallback) {
                searchIssuePicker(id, issueId, currentProjectId, currentIssueTypeId, cfConfigId, queryTerm, resultCallback);
            };
            openPopup(id, popupSearch);
        });

        if (presetValue && !issueId && issueId !== "") {
            // preset single value on create

            var initField = function initField(data) {
                if (data && data.results && data.results.length && data.results.length == 1) {
                    AJS.$('#' + id).auiSelect2('data', data.results[0]);
                }
            };
            queryIssuePicker({
                callback: initField,
                term: ''
            });
            return;
        }

        var initialValues = selectInput$.data('initial-values');
        var initialValues = $.each(initialValues, function(key, value) {
            initialValues[key].displayName = $(value.displayName).text();
       });
        resolveSelectedValues(AJS.$('#' + id).val(), initialValues, cfConfigId, function (data) {
            if (!multiple) {
                if (data && data.length > 0) {
                    // get single item to set
                    data = data[0];
                } else {
                    // empty default option
                    data = { id: '', text: AJS.I18n.getText("common.words.none") };
                }
            }
            AJS.$('#' + id).auiSelect2('data', data);
        });
    }

    function resolveSelectedValues(valueString, initialValues, fieldConfigId, callback) {
        if (! valueString) {
            // nothing selected
            callback([]);
            return;
        }

        // initial values when editing started
        var initialMap = new Map(_(initialValues).map(function (entry) {
            return [ entry.key, entry.displayName ];
        }));
        var selectedValues = [];
        var unresolvedKeys = [];
        // user's selected issue keys at this moment
        var keys = valueString.split(',');
        keys.forEach(function (key) {
            // check if we can get a displayName right now
            var displayName = initialMap.get(key);
            if (displayName) {
                selectedValues.push({ id: key, text: displayName });
            } else {
                unresolvedKeys.push(key);
            }
        });
        if (unresolvedKeys.length == 0) {
            callback(selectedValues);
        } else {
            // perform REST call to resolve issue keys
            var url = AJS.contextPath() +
                '/rest/cwx-issuepicker/latest/issuepicker/resolve-keys?fieldConfigId=' + fieldConfigId +
                '&keys=' + encodeURIComponent(unresolvedKeys.join(','));
            AJS.$.get(url).done(function (result) {
                result.forEach(function (entry) {
                    selectedValues.push({ id: entry.key, text: entry.displayName });
                });
                callback(selectedValues);
            });
        }
    }

    function activateFields() {
        AJS.$('input.cwx-select').each(function(index, element) {
            activateSelect(element);
        });
        activateSortableTable();
    }
    
    function activateSortableTable() {
      var tables = AJS.$(".cwxtable");
      tables.each(function(index){
        var table = AJS.$(this);
        if(!table.hasClass("tablesorter")){
          AJS.tablessortable.setTableSortable(AJS.$(table));
          AJS.$("table.cwxtable>thead>tr>th.tablesorter-header>div.tablesorter-header-inner").each(function(index){
            AJS.$(this).on("click", function(){
              return false;
            });
          });
        }
      });
      
    }

    function openPopup(id, popupSearch) {
        var backgroundBlanket = AJS.$('.aui-blanket').attr('aria-hidden') === 'false';
        AJS.$('body').append(Cwx.IssuePicker.createSelectionPopup());

        var popup = '#cwx-issue-picker-popup';
        AJS.$(popup).on('keydown', function(e) {
            if (e.key === 'Escape') {
                AJS.dialog2(popup).hide();
                e.stopImmediatePropagation();
            }
        });
        AJS.$(popup + ' .cwxip-popup-close').on('click', function() {
            AJS.dialog2(popup).hide();
        });
        AJS.dialog2(popup).on('hide', function() {
            if (backgroundBlanket) {
                // restore blanket after dialog-close handlers have run
                setTimeout(function() {
                    AJS.$('.aui-blanket').css('z-index', '').attr('aria-hidden', 'false');
                }, 0);
            }
        });
        AJS.$(popup + ' .cwxip-popup-search-text').on('keydown', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                performPopupSearch(popupSearch, AJS.$('#' + id).val());
            }
        });
        AJS.$(popup + ' .cwxip-popup-search').on('click', function() {
            performPopupSearch(popupSearch, AJS.$('#' + id).val());
        });
        AJS.$(popup + ' .cwxip-popup-select-all').on('click', function() {
            AJS.$(popup + ' td.cwxip-check-column > input').prop('checked', true);
        });
        AJS.$(popup + ' .cwxip-popup-deselect-all').on('click', function() {
            AJS.$(popup + ' td.cwxip-check-column > input').prop('checked', false);
        });
        AJS.$(popup + ' .cwxip-popup-invert-selection').on('click', function() {
            var checked$ = AJS.$(popup + ' td.cwxip-check-column > input:checked');
            var unchecked$ = AJS.$(popup + ' td.cwxip-check-column > input:not(:checked)');
            checked$.prop('checked', false);
            unchecked$.prop('checked', true);
        });
        AJS.$(popup + ' .cwxip-popup-apply').on('click', function() {
            popupApply(id);
        });

        AJS.dialog2(popup).show();
        // background layer between edit and popup dialog
        AJS.dim(false, 3490);
        AJS.$(popup).css('z-index', 3500);
    }

    function performPopupSearch(popupSearch, currentSelection) {
        AJS.$('#cwxip-spinner').spin();
        var popup = '#cwx-issue-picker-popup';
        var context = AJS.contextPath();
        var selectedOptions = currentSelection.split(',');
        var createTableElement = function createTableElement(issue) {
            var selected = _.contains(selectedOptions, issue.key);
            var html = [];
            html.push('<tr data-id="');
            html.push(issue.key);
            html.push('"><td class="cwxip-check-column"><input type="checkbox"');
            if (selected) {
                // preselect already selected values
                html.push(' checked');
            }
            html.push('></td><td><a href="');
            html.push(context);
            html.push('/browse/');
            html.push(issue.key);
            html.push('" target="_blank" rel="noopener noreferrer">');
            html.push(_.escape(issue.displayName));
            html.push('</a></td></tr>');
            return AJS.$(html.join(''));
        };
        var term = AJS.$(popup + ' .cwxip-popup-search-text').val();
        popupSearch(term, function showResults(data) {
            AJS.$('#cwxip-spinner').spinStop();
            var table$ = AJS.$(popup + ' .cwxip-popup-results-table tbody');
            table$.empty();
            if (data && data.issues) {
                data.issues.forEach(function(element) {
                    table$.append(createTableElement(element));
                });
                if (data.issues.length < data.total) {
                    var message = AJS.I18n.getText("cwx.issue-picker.more-results", data.issues.length, data.total);
                    table$.append(AJS.$('<tr><td></td><td>' + message + '</td></tr>'));
                }
            }
        });
    }

    function popupApply(id) {
        var existingData = AJS.$('#' + id).auiSelect2('data');
        var newData = AJS.$('#cwx-issue-picker-popup td.cwxip-check-column > input:checked').map(function() {
            var row$ = AJS.$(this).parents('tr');
            var key = row$.data('id');
            var text = row$.find('a').text();
            return { id: key, text: text };
        }).toArray();
        AJS.$('#' + id).auiSelect2('data', _.union(existingData, newData));
        AJS.dialog2('#cwx-issue-picker-popup').hide();
    }

    activateFields();
    
    JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, activateFields);
});
