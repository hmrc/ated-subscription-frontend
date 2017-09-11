function showHideIsAnAgentSection() {

    var selectedDiv = $('#hidden-isAnAgent');
    var submitButton = $('#submit');
    var signOutButton = $('#signOut');

    var yesSelected = $('#isAgent-true');
    var noSelected = $('#isAgent-false');

    selectedDiv.hide();
    signOutButton.hide();

    yesSelected.attr('aria-expanded', 'false')
    noSelected.attr('aria-expanded', 'false')

    if($('#isAgent-true').is(':checked')) {
        selectedDiv.show();
    }

    $('input[type=radio][name=isAgent]').change(function(){
        if(this.value == 'true') {
            yesSelected.attr('aria-expanded', 'true')
            selectedDiv.show();
            signOutButton.show();
            submitButton.hide();
        } else {
            noSelected.attr('aria-expanded', 'false')
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

  var yesSelected = $('#appointAgent-true');
  var noSelected = $('#appointAgent-false');

  selectedDivTrue.hide();
  selectedDivFalse.hide();

  yesSelected.attr('aria-expanded', 'false')
  noSelected.attr('aria-expanded', 'false')

  if($('#appointAgent-true').is(':checked')) {
    selectedDivTrue.show();
  }

  if($('#appointAgent-false').is(':checked')) {
    selectedDivFalse.show();
  }

  $('input[type=radio][name=appointAgent]').change(function(){
    if(this.value == 'true') {
      yesSelected.attr('aria-expanded', 'true')
      selectedDivTrue.show();
    } else {
      selectedDivTrue.hide();
    }

    if (this.value == 'false') {
      noSelected.attr('aria-expanded', 'true')
      selectedDivFalse.show();
    } else {
      selectedDivFalse.hide();
    }
  });

}
