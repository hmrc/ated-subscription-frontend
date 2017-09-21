function showHideIsAnAgentSection() {

    var selectedDiv = $('#hidden-isAnAgent');
    var submitButton = $('#submit');
    var signOutButton = $('#signOut');

    selectedDiv.hide();
    signOutButton.hide();


    if($('#isAgent-true').is(':checked')) {
        selectedDiv.show();
    }

    $('input[type=radio][name=isAgent]').change(function(){
        if(this.value == 'true') {
            selectedDiv.show();
            signOutButton.show();
            submitButton.hide();
        } else {
            selectedDiv.hide();
            submitButton.show();
            signOutButton.hide();
        }
    });

}

function showHideAppointAgent() {
  var selectedDivTrue = $('#hidden-appointAgent-true');
  var selectedDivFalse = $('#hidden-appointAgent-false');
  var submitButton = $('#submit');

  selectedDivTrue.hide();
  selectedDivFalse.hide();

  if($('#appointAgent-true').is(':checked')) {
    selectedDivTrue.show();
  }

  if($('#appointAgent-false').is(':checked')) {
    selectedDivFalse.show();
  }

  $('input[type=radio][name=appointAgent]').change(function(){
    if(this.value == 'true') {
      selectedDivTrue.show();
    } else {
      selectedDivTrue.hide();
    }

    if (this.value == 'false') {
      selectedDivFalse.show();
    } else {
      selectedDivFalse.hide();
    }
  });

}
