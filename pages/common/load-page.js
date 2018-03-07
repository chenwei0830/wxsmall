
/**
 * 通用的加载页面
 * config.url 接口地址
 * 
 */
module.exports = function (config) {
    const cfg = Object.assign({ autoLoad: true, methods: {}, data: {} }, config)
    Page(Object.assign({
        data: Object.assign({}, cfg.data),
        onLoad() {
            getApp().editTabBar()
            cfg.autoLoad && this.loadData()
        },
        loadData() {
            let { page = 1, total = 2, list = [], loading = false } = this.data
            console.log(list)
            if (page > total || loading) return
            wx.showLoading({
                title: '加载中...',
            })
            this.setData({ loading: true })

            //此处请求接口
            list.push(...[{}, {}, {}, {}, {}])
            page++
            total = 10
            loading = false
            this.setData({
                page, total, list, loading
            })
            wx.hideLoading()
            wx.stopPullDownRefresh()
        },
        onReachBottom() {
            this.loadData()
        },
        onPullDownRefresh() {
            this.setData({
                page: 1,
                total: 2,
                list: []
            })
            this.loadData()
        }
    }, cfg.methods))
}
