# PhutCityMarket 0.1.0

Plugin quản lý sản phẩm trên Shelf block cho PHUT CITY.

## Features

- Lệnh /marketadmin để mở quản lý kệ.
- OP có thể nhấn chuột phải vào Shelf để mở quản lý.
- Mỗi kệ chứa tối đa 3 sản phẩm.
- Shift-click vật phẩm trong túi đồ để thêm sản phẩm.
- Nhập giá sản phẩm trong chat.
- Click trái sản phẩm để sửa giá.
- Click phải sản phẩm để xóa.
- Lưu dữ liệu trong plugins/PhutCityMarket/config.yml.

## Build

Requirements:
- Java 25
- Gradle 9.2.1
- Paper API 26.3

Run:

gradle clean build

Output:

build/libs/PhutCityMarket-0.1.0.jar

## Install

1. Tắt server.
2. Đưa file JAR vào thư mục plugins.
3. Khởi động lại server.
4. Nhìn vào Shelf block.
5. Dùng /marketadmin hoặc nhấn chuột phải vào Shelf.

Permission:

phutcity.market.admin

Default: OP

## Limitations

Version 0.1.0 hiện chỉ có chức năng quản lý sản phẩm trên kệ.

Chưa có:
- Giỏ hàng khách hàng.
- NPC thu ngân.
- Thanh toán tiền mặt/ngân hàng.
- Trừ hàng tồn kho.
- Hóa đơn.
- Kiểm tra cổng ra siêu thị.
