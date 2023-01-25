function showHideIsAnAgentSection() {

   var signOutButton = $('#signOut');
   var submitButton = $('#submit');
   var hiddenisAgent = $('#hidden-isAnAgent');

   signOutButton.hide();

   $('input[type=radio][name=isAgent]').change(function(){
       if(this.value === 'true') {
          signOutButton.show();
          submitButton.hide();
          hiddenisAgent.show();
       } else {
          submitButton.show();
          hiddenisAgent.hide();
          signOutButton.hide();
       }
   });

}

function showHideAppointAgentSection() {

    var appointAgentTrue = $('#hidden-appointAgent-true');
    var appointAgentFalse = $('#hidden-appointAgent-false');

    $('input[type=radio][name=appointAgent]').change(function(){
        if(this.value === 'true') {
            appointAgentTrue.show();
            appointAgentFalse.hide();
        } else {
            appointAgentTrue.hide();
            appointAgentFalse.show();
        }
    });

}