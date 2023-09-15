function showHideIsAnAgentSection() {

   var signOutButton = $('#signOut');
   var submitButton = $('#submit');
   var hiddenisAgent = $('#hidden-isAnAgent');

   signOutButton.hide();

   $('input[type=radio][name=isAgent]').change(function(){
       if(this.value === 'true') {
          signOutButton.show();
          submitButton.hide();
          hiddenisAgent.removeClass("js-hidden");
       } else {
          submitButton.show();
          hiddenisAgent.addClass("js-hidden");
          signOutButton.hide();
       }
   });

}

function showHideAppointAgentSection() {

    var appointAgentTrue = $('#hidden-appointAgent-true');
    var appointAgentFalse = $('#hidden-appointAgent-false');

    $('input[type=radio][name=appointAgent]').change(function(){
        if(this.value === 'true') {
            appointAgentTrue.removeClass("js-hidden");
            appointAgentFalse.addClass("js-hidden");;
        } else {
            appointAgentTrue.addClass("js-hidden");
            appointAgentFalse.removeClass("js-hidden");
        }
    });

}