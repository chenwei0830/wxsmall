Page({
  data: {
    textAreaObj:{
      fontNum:0
    },
    btnSubmitClazz:{
      btnImg:"../../../icons/btn-submit_disabled.png"
    },
    btnBackClazz: {
      btnImg: "../../../icons/btn-back_active.png"
    }
  },
  btnComplete : function(ev){
    var v = ev.detail.value;
    if(v.length>0){
      this.setData({
        textAreaObj: {
          fontNum: v.length
        },
        btnSubmitClazz: {
          btnImg: "../../../icons/btn-submit_active.png"
        }
      })
    }else{
      this.setData({
        textAreaObj: {
          fontNum: 0
        },
        btnSubmitClazz: {
          btnImg: "../../../icons/btn-submit_disabled.png"
        }
      })
    }
  }
});