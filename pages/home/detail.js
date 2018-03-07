Page({

    /**
     * 页面的初始数据
     */
    data: {
        content: {
            id: 2,
            avatar: 'http://oj1itelvn.bkt.clouddn.com/timg.jpg',
            name: 'SHERRY & MOLLY',
            label: '摄影',
            grade: 1,
            address: '四川省成都市高新区腾讯大厦哈哈哈哈或',
            date: '09-13',
            time: '09:42',
            title: '阿道夫洒洒地发生地方，艾丝凡考核撒的空间返回睡大觉阿贾克斯地方华盛顿',
            click:12,
            music:{
                src: 'http://ws.stream.qqmusic.qq.com/M500001VfvsJ21xFqb.mp3?guid=ffffffff82def4af4b12b3cd9337d5e7&uin=346897220&vkey=6292F51E1E384E06DCBDC9AB7C49FD713D632D313AC4858BACB8DDD29067D3C601481D36E62053BF8DFEAF74C0A5CCFADD6471160CAF3E6A&fromtag=46',
                name:'音乐',
                author:'未知作者',
                image:'http://oj1itelvn.bkt.clouddn.com/timg.jpg'
            },
            image: [
                'http://oj1itelvn.bkt.clouddn.com/hhh.jpg',
                'http://oj1itelvn.bkt.clouddn.com/art/artist-bg.png'
            ],
            video: 'http://oj1itelvn.bkt.clouddn.com/art/test-mp4.mp4',
            is_keep: 0,
            artComment: [
                {
                    avatar: 'http://oj1itelvn.bkt.clouddn.com/timg.jpg',
                    name: '像个杂货铺111',
                    label: '画家',
                    detail: '索拉卡的叫法思考四大件发生考虑到，桑德菲杰sd卡家乐福圣诞节放假撒快递放假。',
                    date: '01/05',
                    time: '10:55',
                    num_like: 0
                },
                {
                    avatar: 'http://oj1itelvn.bkt.clouddn.com/timg.jpg',
                    name: '哈哈哈',
                    label: '画家',
                    detail: '索拉卡的叫法思考四大件发生考虑到，桑德菲杰sd卡家乐福圣诞节放假撒快递放假。',
                    date: '01/05',
                    time: '10:55',
                    num_like: 0
                }
            ],
            commentList: [
                {
                    avatar: 'http://oj1itelvn.bkt.clouddn.com/timg.jpg',
                    name: '像个杂货铺',
                    label: '画家',
                    detail: '索拉卡的叫法思考四大件发生考虑到，桑德菲杰sd卡家乐福圣诞节放假撒快递放假。',
                    date: '01/05',
                    time: '10:55',
                    num_like: 5,
                },
                {
                    avatar: 'http://oj1itelvn.bkt.clouddn.com/timg.jpg',
                    name: '像个杂货铺',
                    label: '画家',
                    detail: '索拉卡的叫法思考四大件发生考虑到，桑德菲杰sd卡家乐福圣诞节放假撒快递放假。',
                    date: '01/05',
                    time: '10:55',
                    num_like: 0,
                }
            ],
            is_comment: 0,
            is_like: 0,
            is_focus:0
        },
        current: 1
    },
    currentClick(evt){
        this.setData({
            current: evt.currentTarget.dataset.current
        })
    },

    watch(evt){
        this.setData({
            "content.is_watch":!this.data.content.is_watch
        })
        //请求接口
    },

    /**
     * 用户点击右上角分享
     */
    onShareAppMessage: function () {
        return {
            title: "xxx",//分享名称
        }
    },
    like(evt){
        this.setData({
            "content.is_like": !this.data.content.is_like
        })
    },
    keep(evt){
        this.setData({
            "content.is_keep": !this.data.content.is_keep
        })
    },
    likeComment1(evt){
        const {index} = evt.currentTarget.dataset
        this.setData({
            [`content.commentList[${index}].is_like`]: !this.data.content.commentList[index].is_like
        })
    },
    likeComment2(evt) {
        const { index } = evt.currentTarget.dataset
        this.setData({
            [`content.artComment[${index}].is_like`]: !this.data.content.artComment[index].is_like
        })
    },
    // 查看大图
    preImg(evt) {
        let { current, imgs } = evt.currentTarget.dataset;
        wx.previewImage({
            urls: imgs.map(o => {
                return o
            }),
            current
        })
    },
})