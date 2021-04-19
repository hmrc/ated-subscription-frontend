function showHideIsAnAgentSection() {

   var signOutButton = $('#signOut');
   var submitButton = $('#submit');

   signOutButton.hide();

   $('input[type=radio][name=isAgent]').change(function(){
       if(this.value == 'true') {
          signOutButton.show();
          submitButton.hide();
       } else {
          submitButton.show();
          signOutButton.hide();
       }
   });

}

