AJS.toInit(function() {
    function selectionModeChanged() {
        var selectionMode = AJS.$('input[name=selectionMode]:checked').val();
        // presetValue and fieldsToCopy only for single
        AJS.$('#presetValue').prop('disabled', selectionMode !== 'SINGLE');
        AJS.$('#fieldsToCopy').prop('disabled', selectionMode !== 'SINGLE');
        AJS.$('#copyFieldMapping').prop('disabled', selectionMode !== 'SINGLE');
        // fieldsToDisplay + sumUpFields only for table variants
        AJS.$('#fieldsToDisplay').prop('disabled', selectionMode === 'SINGLE');
        AJS.$('#sumUpFields').prop('disabled', selectionMode === 'SINGLE');
        AJS.$('#indexTableFields').prop('disabled', selectionMode === 'SINGLE');
        AJS.$('#expandIssueTable').prop('disabled', selectionMode === 'SINGLE');
        AJS.$('input[name=linkMode]').prop('disabled', selectionMode === 'NONE');
    }
    
    function copyFieldMappingChanged(){
      //check section mode
      var selectionMode = AJS.$('input[name=selectionMode]:checked').val();
      if(selectionMode === 'SINGLE'){
          var copyFieldMappingValue =  AJS.$('select[name=copyFieldMapping]').val();
          AJS.$('#fieldsToCopy').prop('disabled', copyFieldMappingValue !== 'EMPTY');
      }
    }
    
    function initFieldMappingChanged(){
      var createNew = AJS.$('input[name=createNewValue]').prop('checked');
      if(createNew){
        var initFieldMappingValue =  AJS.$('select[name=initFieldMapping]').val();
        AJS.$('#fieldsToInit').prop('disabled', initFieldMappingValue !== 'EMPTY');
      }
    }

    function currentProjectChanged(){
      var createNew = AJS.$('input[name=createNewValue]').prop('checked');
      if(createNew){
        var currentProjectValue =  AJS.$('input[name=currentProject]').prop('checked');
        AJS.$('select[name=newIssueProject]').prop('disabled', currentProjectValue);
      }
    }
    
    function displayModeChanged() {
        var displayMode = AJS.$('input[name=displayMode]:checked').val();
        AJS.$('#showIssueKey').prop('disabled', displayMode !== 'SINGLE_ATTRIBUTE');
    }

    function linkModeChanged() {
        var linkMode = AJS.$('input[name=linkMode]:checked').val();
        // linkType only if linkMode != NONE
        AJS.$('select[name=linkType]').prop('disabled', linkMode === 'NONE');
    }

    function createNewValueChanged() {
        var createNew = AJS.$('input[name=createNewValue]').prop('checked');
        AJS.$('select[name=newIssueProject]').prop('disabled', ! createNew);
        AJS.$('input[name=currentProject]').prop('disabled', ! createNew);
        AJS.$('select[name=newIssueType]').prop('disabled', ! createNew);
        AJS.$('select[name=fieldsToInit]').prop('disabled', ! createNew);
        AJS.$('select[name=initFieldMapping]').prop('disabled', ! createNew);
    }

    AJS.$('#fieldsToDisplay').auiSelect2();
    AJS.$('#sumUpFields').auiSelect2();
    AJS.$('#fieldsToCopy').auiSelect2();
    AJS.$('#fieldsToInit').auiSelect2();

    
    AJS.$('input[name=selectionMode]').on('change', selectionModeChanged);
    AJS.$('input[name=displayMode]').on('change', displayModeChanged);
    AJS.$('input[name=linkMode]').on('change', linkModeChanged);
    AJS.$('input[name=createNewValue]').on('change', createNewValueChanged);
    AJS.$('input[name=currentProject]').on('change', currentProjectChanged);
    AJS.$('select[name=copyFieldMapping]').on('change', copyFieldMappingChanged);
    AJS.$('select[name=initFieldMapping]').on('change', initFieldMappingChanged);

    
    selectionModeChanged();
    displayModeChanged();
    linkModeChanged();
    createNewValueChanged();
    copyFieldMappingChanged();
    currentProjectChanged();
    initFieldMappingChanged();
});
