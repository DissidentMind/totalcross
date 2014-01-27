#include "pch.h"
#include "PhoneDirect3DXamlAppComponent.h"
#include "Direct3DContentProvider.h"

using namespace Windows::Foundation;
using namespace Windows::UI::Core;
using namespace Microsoft::WRL;
using namespace Windows::Phone::Graphics::Interop;
using namespace Windows::Phone::Input::Interop;

namespace PhoneDirect3DXamlAppComponent
{

	Direct3DBackground::Direct3DBackground(Idummy ^_odummy) : odummy(_odummy)
{
}

IDrawingSurfaceBackgroundContentProvider^ Direct3DBackground::CreateContentProvider()
{
	ComPtr<Direct3DContentProvider> provider = Make<Direct3DContentProvider>(this);
	return reinterpret_cast<IDrawingSurfaceBackgroundContentProvider^>(provider.Get());
}

// IDrawingSurfaceManipulationHandler
void Direct3DBackground::SetManipulationHost(DrawingSurfaceManipulationHost^ manipulationHost)
{
	manipulationHost->PointerPressed +=
		ref new TypedEventHandler<DrawingSurfaceManipulationHost^, PointerEventArgs^>(this, &Direct3DBackground::OnPointerPressed);

	manipulationHost->PointerMoved +=
		ref new TypedEventHandler<DrawingSurfaceManipulationHost^, PointerEventArgs^>(this, &Direct3DBackground::OnPointerMoved);

	manipulationHost->PointerReleased +=
		ref new TypedEventHandler<DrawingSurfaceManipulationHost^, PointerEventArgs^>(this, &Direct3DBackground::OnPointerReleased);
}

// Event Handlers
void Direct3DBackground::OnPointerPressed(DrawingSurfaceManipulationHost^ sender, PointerEventArgs^ args)
{
	// Insert your code here.
}

void Direct3DBackground::OnPointerMoved(DrawingSurfaceManipulationHost^ sender, PointerEventArgs^ args)
{
	// Insert your code here.
}

void Direct3DBackground::OnPointerReleased(DrawingSurfaceManipulationHost^ sender, PointerEventArgs^ args)
{
	// Insert your code here.
}

// Interface With Direct3DContentProvider
HRESULT Direct3DBackground::Connect(_In_ IDrawingSurfaceRuntimeHostNative* host, _In_ ID3D11Device1* device)
{
   m_renderer = ref new Direct3DBase();
	m_renderer->Initialize(device);
	m_renderer->UpdateForWindowSizeChange(WindowBounds.Width, WindowBounds.Height);
	return S_OK;
}

void Direct3DBackground::Disconnect()
{
	m_renderer = nullptr;
}

HRESULT Direct3DBackground::PrepareResources(_In_ const LARGE_INTEGER* presentTargetTime, _Inout_ DrawingSurfaceSizeF* desiredRenderTargetSize)
{
	desiredRenderTargetSize->width = RenderResolution.Width;
	desiredRenderTargetSize->height = RenderResolution.Height;

	return S_OK;
}

HRESULT Direct3DBackground::Draw(_In_ ID3D11Device1* device, _In_ ID3D11DeviceContext1* context, _In_ ID3D11RenderTargetView* renderTargetView)
{
	static int x = 0;
	m_renderer->UpdateDevice(device, context, renderTargetView);
	if (!m_renderer->Render()) {
	   RequestAdditionalFrame();
	} else {
	   RequestAdditionalFrame();
	   x++;
	}

	/*while (x > 10) {
		m_renderer->Render();
		RequestAdditionalFrame();
		odummy->callDraw();
	}*/

	return S_OK;
}

}