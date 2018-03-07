// pages/auth/index.js
const app = getApp()
const time = 60
Page({

    /**
     * 页面的初始数据
     */
    data: {
        codeTime: time,
        waitTime: time,
        type: [
            { name: '分类1' },
            { name: '分类2' },
            { name: '分类3' },
        ],//艺术分类
        level: [
            { name: '分类1' },
            { name: '分类2' },
            { name: '分类3' },
        ],//艺术级别
        post: {},
        checked:true,
        sslList:[]
    },
    onLoad(){
        // 请求接口，获取价格

        wx.getSystemInfo({
            success: res => {
                this.setData({
                    imgSize: Math.floor((res.windowWidth - 40) / 3)
                })
            },
        })
    },
    onInput: function (evt) {
        var key = evt.currentTarget.dataset.key;
        var val = evt.detail.value;
        this.setData({
            [`post.${key}`]:val
        })
    },
    typeChange(evt) {
        const { key } = evt.currentTarget.dataset
        this.setData({
            [`post.${key}`]: evt.detail.value,
        })
    },
    addSSL() {
        wx.chooseImage({
            success: res => {
                const { sslList = [] } = this.data
                sslList.push(...res.tempFilePaths)
                this.setData({ sslList })
            },
        })
    },

    getCode() {
        if (!this.data.post.phone){
            app.wxToast.warn('请输入手机号码');
            return;
        }
        if (!this.data.post.phone.match(/^1[3|4|5|7|8][0-9]\d{4,8}$/)) {
            app.wxToast.warn('请输入正确手机号');
            return;
        }
        let { codeTime, waitTime } = this.data
        if (codeTime != waitTime) {
            return
        }
        codeTime--
        this.setData({ codeTime })
        //获取验证码接口
        console.log("get-code")
        //倒计时
        var tid = setInterval(() => {
            codeTime--
            if (codeTime <= 0) {
                codeTime = waitTime
                clearInterval(tid)
            }
            this.setData({ codeTime })
        }, 1000)


    },
    check(){
        this.setData({
            checked:!this.data.checked
        })
    },

    // 选择图片
    chooseImg() {
        wx.chooseImage({
            success: res => {
                const { imgList=[] } = this.data
                imgList.push(...res.tempFilePaths)
                this.setData({ imgList })
            },
        })
    },
    imgClick(evt) {
        wx.showActionSheet({
            itemList: ['预览', '删除'],
            success: res => {
                const { index, src } = evt.currentTarget.dataset
                if (res.tapIndex == 0) {
                    wx.previewImage({
                        urls: [src],
                    })
                }
            
                if (res.tapIndex == 1) {
                    const { imgList } = this.data
                    imgList.splice(index, 1)
                    this.setData({ imgList })
                }
            }
        })
    },

    submit: function () {
        const { post } = this.data
        
        if (!post.name) {
            app.wxToast.warn('请输入姓名');
            return;
        }
        if (!post.card) {
            app.wxToast.warn('请输入身份证号码');
            return;
        }
        if (!post.card.match(/^\d{6}(18|19|20)?\d{2}(0[1-9]|1[12])(0[1-9]|[12]\d|3[01])\d{3}(\d|X)$/i)) {
            app.wxToast.warn('请输入正确的身份证号码');
            return;
        }
        if (!post.phone) {
            app.wxToast.warn('请输入联系电话');
            return;
        }
        if (!post.phone.match(/^1[3|4|5|7|8][0-9]\d{4,8}$/)) {
            app.wxToast.warn('请输入正确手机号');
            return;
        }
        if (!post.code) {
            app.wxToast.warn('请输入短信验证码');
            return;
        }
        if (post.selectedType === undefined) {
            app.wxToast.warn('请选择您的专业分类');
            return;
        }
        if (post.selectedLevel === undefined) {
            app.wxToast.warn('请选择您的艺术级别');
            return;
        }
        if (!this.data.sslList.length){
            app.wxToast.warn('请添加证书');
            return;
        }
        if (this.data.checked==false) {
            app.wxToast.warn('请查看合约条款');
            return;
        }
        // 请求支付,接口
        wx.requestPayment({
            timeStamp: '',
            nonceStr: '',
            package: '',
            signType: '',
            paySign: '',
        })
    },

})