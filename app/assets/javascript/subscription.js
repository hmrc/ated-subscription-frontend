function showHideIsAnAgentSection() {

   var signOutButton = $('#signOut');
   var submitButton = $('#submit');
   var hiddenisAgent = $('#hidden-isAnAgent');

   signOutButton.hide();

   $('input[type=radio][name=isAgent]').change(function(){
       if(this.value === 'true') {
          signOutButton.show();
          submitButton.hide();
          hiddenisAgent.addClass("js-visible");
       } else {
          submitButton.show();
          hiddenisAgent.removeClass("js-visible");
          signOutButton.hide();
       }
   });

}

function showHideAppointAgentSection() {

    var appointAgentTrue = $('#hidden-appointAgent-true');
    var appointAgentFalse = $('#hidden-appointAgent-false');

    $('input[type=radio][name=appointAgent]').change(function(){
        if(this.value === 'true') {
            appointAgentTrue.addClass("js-visible");
            appointAgentFalse.removeClass("js-visible");
        } else {
            appointAgentTrue.removeClass("js-visible");
            appointAgentFalse.addClass("js-visible");
        }
    });

}