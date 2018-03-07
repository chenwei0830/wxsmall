// pages/pub/index.js
Page({

    /**
     * 页面的初始数据
     */
    data: {
        current: 1,
        imgList: [
            'http://oj1itelvn.bkt.clouddn.com/test.jpg',
            'http://oj1itelvn.bkt.clouddn.com/banner-4.png',
        ],
        audioList: [
            { name: "无" },
            { name: "你好", src: "http://ws.stream.qqmusic.qq.com/M500001VfvsJ21xFqb.mp3?guid=ffffffff82def4af4b12b3cd9337d5e7&uin=346897220&vkey=6292F51E1E384E06DCBDC9AB7C49FD713D632D313AC4858BACB8DDD29067D3C601481D36E62053BF8DFEAF74C0A5CCFADD6471160CAF3E6A&fromtag=46" },
            { name: "你好", src: "http://ws.stream.qqmusic.qq.com/M500001VfvsJ21xFqb.mp3?guid=ffffffff82def4af4b12b3cd9337d5e7&uin=346897220&vkey=6292F51E1E384E06DCBDC9AB7C49FD713D632D313AC4858BACB8DDD29067D3C601481D36E62053BF8DFEAF74C0A5CCFADD6471160CAF3E6A&fromtag=46" }
        ],
        isModal: false,
        type: [
            { name: '分类1' },
            { name: '分类2' },
            { name: '分类3' },
        ],//艺术分类
        post: {
        }
    },

    typeChange(evt) {
        console.log(evt)
        this.setData({
            [`post[${this.data.current}].selectedType`]: evt.detail.value
        })
    },
    openMap() {
        wx.chooseLocation({
            success: res => {
                this.setData({
                    [`post[${this.data.current}].address`]: res.address
                })
            },
        })
    },

    continueEdit(evt) {
        if (!this.data.modalContent) {
            return
        }
        wx.showModal({
            title: '提示',
            content: '确定继续编写未完成的文章吗？',
            cancelText: '忽略',
            confirmText: '继续',
            success: res => {
                if (res.confirm) {
                    this.setData({
                        [`post[${this.data.current}].text`]: this.data.modalContent
                    })
                    this.closeModal()

                } else if (res.cancel) {
                    wx.removeStorage({
                        key: 'lastText',
                        success: function (res) {
                            console.log(res.data)
                        }
                    })
                    this.closeModal()
                }
            }
        })
    },
    closeModal() {
        this.setData({
            isModal: false,
            isAudio: false
        })
        // wx.stopBackgroundAudio()
    },
    showModal() {
        this.setData({
            isModal: true
        })
    },

    /**
     * 生命周期函数--监听页面加载
     */
    onLoad: function (options) {
        wx.getSystemInfo({
            success: res => {
                this.setData({
                    imgSize: Math.floor((res.windowWidth - 40) / 3)
                })
            },
        })
    },
    setCurrent(evt) {
        const { current } = evt.currentTarget.dataset
        this.setData({
            current
        })
        if(current==2){
            wx.stopBackgroundAudio();
        }
        if (current == 3 && (!this.data.post[3] || !this.data.post[3].text)) {
            //获取缓存
            wx.getStorage({
                key: 'lastText',
                success: res => {
                    if (!res.data.trim()) {
                        return
                    }
                    this.setData({
                        modalContent: res.data
                    })
                    this.showModal()
                }
            })
        }
        // getApp().fbCurrent = evt.currentTarget.dataset.current
    },
    chooseImg() {
        wx.chooseImage({
            success: res => {
                const { imgList } = this.data
                imgList.push(...res.tempFilePaths)
                this.setData({ imgList })
            },
        })
    },
    chooseVideo() {
        wx.chooseVideo({
            success: res => {
                this.setData({
                    "post.video": res.tempFilePath
                })
            }
        })
    },

    chooseAudio() {
        this.setData({
            isAudio: true
        })
    },
    clickAudio(evt) {
        const { index } = evt.currentTarget.dataset
        this.setData({
            "post.audio": index
        })
        const { src } = this.data.audioList[index]
        src && wx.playBackgroundAudio({
            dataUrl: src
        })
        this.setData({
            dataUrl: this.data.audioList[index]
        })
        !src && wx.stopBackgroundAudio()
    },


    // pcClick() {
    //     this.setData({
    //         current: 4
    //     })

    // },

    onInput(evt) {
        this.setData({
            [`post[${this.data.current}].title`]: evt.detail.value
        })
    },
    onText(evt) {
        this.setData({
            [`post[${this.data.current}].text`]: evt.detail.value
        })
    },
    submit() {
        const postData = this.data.post[this.data.current]
        if (!postData) {
            getApp().wxToast.warn("请输入标题")
            return
        }
        if (!postData.title) {
            getApp().wxToast.warn("请输入标题")
            return
        }
        if (!postData.selectedType) {
            getApp().wxToast.warn("请选择艺术分类")
            return
        }
        if (!postData.address) {
            getApp().wxToast.warn("请选择位置")
            return
        }


        //发布完成后清除缓存
        wx.removeStorage({
            key: 'lastText',
        })
    },

    saveCache(evt) {
        wx.setStorage({
            key: 'lastText',
            data: evt.detail.value,
        })
    },
    imgClick(evt) {
        wx.showActionSheet({
            itemList: ['预览', '设为封面', '删除'],
            success: res => {
                const { index, src } = evt.currentTarget.dataset
                if (res.tapIndex == 0) {
                    wx.previewImage({
                        urls: [src],
                    })
                }
                if (res.tapIndex == 1) {
                    const { imgList } = this.data
                    imgList.unshift(imgList.splice(index, 1));
                    this.setData({ imgList })
                }
                if (res.tapIndex == 2) {
                    const { imgList } = this.data
                    imgList.splice(index, 1)
                    this.setData({ imgList })
                }
            }
        })
    }
})