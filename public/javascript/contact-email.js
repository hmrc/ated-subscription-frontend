function showHideEmailFunc() {

  var checkBoxValue = $('#' + "emailConsent-true" + ':checked').val();

  var emailContact = $("#email-contact-hidden");

  var email = $('#email');

  emailContact.hide();

  if(checkBoxValue == 'true') {
    emailContact.show();
  }

  $('input[type=radio][name=emailConsent]').change(function(){
    if(this.value == 'true') {
      emailContact.show();
    } else {
      emailContact.hide();
      email.val("");
    }
  });

}

$(document).ready(function() {
  showHideEmailFunc();
});